package org.nishen.resourcepartners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.dao.Config;
import org.nishen.resourcepartners.dao.ConfigFactory;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.entity.ElasticSearchChangeRecord;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.entity.ElasticSearchPartnerAddress;
import org.nishen.resourcepartners.entity.ElasticSearchSuspension;
import org.nishen.resourcepartners.entity.SyncPayload;
import org.nishen.resourcepartners.model.Address;
import org.nishen.resourcepartners.model.Addresses;
import org.nishen.resourcepartners.model.ContactInfo;
import org.nishen.resourcepartners.model.Email;
import org.nishen.resourcepartners.model.Email.EmailTypes;
import org.nishen.resourcepartners.model.Emails;
import org.nishen.resourcepartners.model.IsoDetails;
import org.nishen.resourcepartners.model.Note;
import org.nishen.resourcepartners.model.ObjectFactory;
import org.nishen.resourcepartners.model.Partner;
import org.nishen.resourcepartners.model.PartnerDetails;
import org.nishen.resourcepartners.model.PartnerDetails.LocateProfile;
import org.nishen.resourcepartners.model.PartnerDetails.SystemType;
import org.nishen.resourcepartners.model.Phone;
import org.nishen.resourcepartners.model.Phone.PhoneTypes;
import org.nishen.resourcepartners.model.Phones;
import org.nishen.resourcepartners.model.ProfileDetails;
import org.nishen.resourcepartners.model.ProfileType;
import org.nishen.resourcepartners.model.RequestExpiryType;
import org.nishen.resourcepartners.model.Status;
import org.nishen.resourcepartners.util.JaxbUtil;
import org.nishen.resourcepartners.util.JaxbUtilModel;
import org.nishen.resourcepartners.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SyncProcessorImpl implements SyncProcessor
{
	private static final Logger log = LoggerFactory.getLogger(SyncProcessorImpl.class);

	private static final String REGEX_SAMEAS = "same as (.*) address";

	private static final String DEFAULT_LINK_BASE = "https://api-ap.hosted.exlibrisgroup.com/almaws/v1/partners/";

	private Config config;

	private ElasticSearchDAO elastic;

	private AlmaDAO alma;

	private String nuc;

	private ObjectFactory of = new ObjectFactory();

	private Pattern patternSameAs = Pattern.compile(REGEX_SAMEAS, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@Inject
	public SyncProcessorImpl(ElasticSearchDAO elastic, AlmaDAOFactory almaFactory, ConfigFactory configFactory,
	                         @Assisted("nuc") String nuc, @Assisted("apikey") String apikey)
	{
		this.config = configFactory.fetch(nuc, false);
		this.elastic = elastic;
		this.alma = almaFactory.create(apikey);
		this.nuc = nuc;

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	public Optional<SyncPayload> sync(boolean preview) throws SyncException
	{
		log.debug("sync nuc: {}", nuc);

		if (config.isEmpty())
			throw new SyncException("configuration does not exist: " + nuc);

		Map<String, ElasticSearchPartner> elasticPartners = elastic.getPartners();
		log.debug("elasticsearch partners found: {}", elasticPartners.size());

		Map<String, Partner> almaPartners = alma.getPartners();
		log.debug("alma partners found: {}", almaPartners.size());

		Map<String, Partner> changed = new HashMap<String, Partner>();
		Map<String, Partner> deleted = new HashMap<String, Partner>();
		Map<String, List<ElasticSearchChangeRecord>> allChanges =
		        new HashMap<String, List<ElasticSearchChangeRecord>>();

		long allChangesCount = 0;

		List<String> remaining = new ArrayList<String>(almaPartners.keySet());
		for (String s : elasticPartners.keySet())
		{
			remaining.remove(s);

			// skip own org
			if (s.equals(nuc))
				continue;

			log.debug("processing org: {}", s);

			Partner a = makePartner(elasticPartners.get(s));
			log.debug("elasticPartner[{}]: {}", s, JaxbUtilModel.formatPretty(a));

			Partner b = almaPartners.get(s);
			if (b != null)
			{
				// Check for NOSYNC in notes
				boolean nosync = false;
				for (Note note : b.getNotes().getNote())
					if (note != null && "NOSYNC".equalsIgnoreCase(note.getContent()))
						nosync = true;

				// skip record if we have a NOSYNC flag.
				if (nosync)
					continue;

				// we keep notes from Alma - source of truth for notes.
				a.setNotes(b.getNotes());
				log.debug("almaPartner[{}]: {}", s, JaxbUtilModel.formatPretty(b));
			}
			else
			{
				log.debug("almaPartner[{}]: {}", s, "new Partner");
			}

			List<ElasticSearchChangeRecord> changes = comparePartners(a, b);
			log.debug("comparing partners [{}], changecount: {}", s, changes.size());

			if (changes.size() > 0)
			{
				// set the nuc (partner) that this change was made for
				for (ElasticSearchChangeRecord c : changes)
					c.setNuc(s);

				allChanges.put(s, changes);
				allChangesCount += changes.size();
				changed.put(s, a);
			}
		}

		try
		{
			if (log.isDebugEnabled())
				for (String nuc : allChanges.keySet())
					log.debug("changes [{}]: {}", nuc, JaxbUtil.formatPretty(allChanges.get(nuc)));

			List<ElasticSearchChangeRecord> changeRecords = new ArrayList<ElasticSearchChangeRecord>();
			for (String nuc : allChanges.keySet())
				changeRecords.addAll(allChanges.get(nuc));

			for (String r : remaining)
				deleted.put(r, almaPartners.get(r));

			if (!preview)
			{
				elastic.addEntities(changeRecords);
				alma.savePartners(changed);
			}
		}
		catch (Exception e)
		{
			log.error("failed to save change records: {}", e.getMessage(), e);
		}

		log.debug("payload: changed={}, changes={}, deleted={}", changed.size(), allChangesCount, deleted.size());

		return Optional.of(new SyncPayload(changed, deleted, allChanges));
	}

	@Override
	public List<ElasticSearchChangeRecord> comparePartners(Partner a, Partner b)
	{
		assert (a != null);

		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (b == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "partner", null, JaxbUtilModel.format(a)));
			return changes;
		}

		if (!compareStrings(a.getLink(), b.getLink()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "link", b.getLink(), a.getLink()));

		changes.addAll(compareContactInfo(a.getContactInfo(), b.getContactInfo()));
		changes.addAll(comparePartnerDetails(a.getPartnerDetails(), b.getPartnerDetails()));

		return changes;
	}

	private List<ElasticSearchChangeRecord> compareContactInfo(ContactInfo a, ContactInfo b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		changes.addAll(compareAddresses(a.getAddresses(), b.getAddresses()));
		changes.addAll(compareEmails(a.getEmails(), b.getEmails()));
		changes.addAll(comparePhones(a.getPhones(), b.getPhones()));

		return changes;
	}

	private List<ElasticSearchChangeRecord> compareAddresses(Addresses a, Addresses b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			for (Address address : b.getAddress())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "address", JaxbUtilModel.format(address), null));
			return changes;
		}

		if (b == null)
		{
			for (Address address : a.getAddress())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "address", null, JaxbUtilModel.format(address)));
			return changes;
		}

		// pre comparison work:
		// - sorting because comparison order is critical.
		// - address start date is not compared, elastic is null, so setting alma to null as well.
		for (Address address : b.getAddress())
		{
			Collections.sort(address.getAddressTypes().getAddressType());
			address.setStartDate(null);
		}

		for (Address address : a.getAddress())
			if (!b.getAddress().contains(address))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "address", null, JaxbUtilModel.format(address)));

		for (Address address : b.getAddress())
			if (!a.getAddress().contains(address))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "address", JaxbUtilModel.format(address), null));

		return changes;
	}

	private List<ElasticSearchChangeRecord> compareEmails(Emails a, Emails b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			for (Email email : b.getEmail())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "email", JaxbUtilModel.format(email), null));
			return changes;
		}

		if (b == null)
		{
			for (Email email : a.getEmail())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "email", null, JaxbUtilModel.format(email)));
			return changes;
		}

		// pre comparison work:
		// - sorting because comparison order is critical.
		for (Email email : b.getEmail())
			Collections.sort(email.getEmailTypes().getEmailType());

		for (Email email : a.getEmail())
			if (!b.getEmail().contains(email))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "email", null, JaxbUtilModel.format(email)));

		for (Email email : b.getEmail())
			if (!a.getEmail().contains(email))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "email", JaxbUtilModel.format(email), null));

		return changes;
	}

	private List<ElasticSearchChangeRecord> comparePhones(Phones a, Phones b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			for (Phone phone : b.getPhone())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "phone", JaxbUtilModel.format(phone), null));
			return changes;
		}

		if (b == null)
		{
			for (Phone phone : a.getPhone())
				changes.add(new ElasticSearchChangeRecord(nuc, null, "phone", null, JaxbUtilModel.format(phone)));
			return changes;
		}

		// pre comparison work:
		// - sorting because comparison order is critical.
		for (Phone phone : b.getPhone())
			Collections.sort(phone.getPhoneTypes().getPhoneType());

		for (Phone phone : a.getPhone())
			if (!b.getPhone().contains(phone))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "phone", null, JaxbUtilModel.format(phone)));

		for (Phone phone : b.getPhone())
			if (!a.getPhone().contains(phone))
				changes.add(new ElasticSearchChangeRecord(nuc, null, "phone", JaxbUtilModel.format(phone), null));

		return changes;
	}

	private List<ElasticSearchChangeRecord> comparePartnerDetails(PartnerDetails a, PartnerDetails b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "partnerDetails", JaxbUtilModel.format(b), null));
			return changes;
		}

		if (b == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "partnerDetails", null, JaxbUtilModel.format(a)));
			return changes;
		}

		if (a.getAvgSupplyTime() != b.getAvgSupplyTime())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "avgSupplyTime",
			                                          Integer.toString(b.getAvgSupplyTime()),
			                                          Integer.toString(a.getAvgSupplyTime())));

		if (a.isBorrowingSupported() != b.isBorrowingSupported())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "borrowingSupported",
			                                          Boolean.toString(b.isBorrowingSupported()),
			                                          Boolean.toString(a.isBorrowingSupported())));

		if (!compareStrings(a.getBorrowingWorkflow(), b.getBorrowingWorkflow()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "borrowingWorkflow", b.getBorrowingWorkflow(),
			                                          a.getBorrowingWorkflow()));

		if (!compareStrings(a.getCode(), b.getCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "code", b.getCode(), a.getCode()));

		if (!compareStrings(a.getCurrency(), b.getCurrency()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "currency", b.getCurrency(), a.getCurrency()));

		if (a.getDeliveryDelay() != b.getDeliveryDelay())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "deliveryDelay",
			                                          Integer.toString(b.getDeliveryDelay()),
			                                          Integer.toString(a.getDeliveryDelay())));

		if (!compareStrings(a.getHoldingCode(), b.getHoldingCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "holdingCode", b.getHoldingCode(),
			                                          a.getHoldingCode()));

		if (!compareStrings(a.getInstitutionCode(), b.getInstitutionCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "institutionCode", b.getInstitutionCode(),
			                                          a.getInstitutionCode()));

		if (a.isLendingSupported() != b.isLendingSupported())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "lendingSupported",
			                                          Boolean.toString(b.isLendingSupported()),
			                                          Boolean.toString(a.isLendingSupported())));

		if (!compareStrings(a.getLendingWorkflow(), b.getLendingWorkflow()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "lendingWorkflow", b.getLendingWorkflow(),
			                                          a.getLendingWorkflow()));

		changes.addAll(compareValueDescPair("locateProfile", a.getLocateProfile(), b.getLocateProfile()));

		if (!compareStrings(a.getName(), b.getName()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "name", b.getName(), a.getName()));

		if (!a.getStatus().equals(b.getStatus()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "status", b.getStatus().toString(),
			                                          a.getStatus().toString()));

		changes.addAll(compareValueDescPair("systemType", a.getSystemType(), b.getSystemType()));

		// TODO: check null profile details?
		switch (a.getProfileDetails().getProfileType())
		{
			case ISO:
				changes.addAll(compareIsoDetails(a.getProfileDetails().getIsoDetails(),
				                                 b.getProfileDetails().getIsoDetails()));
				break;

			default:
		}

		return changes;
	}

	private <T> List<ElasticSearchChangeRecord> compareValueDescPair(String fieldname, T a, T b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, fieldname, JaxbUtilModel.format(b), null));
			return changes;
		}

		if (b == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, fieldname, null, JaxbUtilModel.format(a)));
			return changes;
		}

		if (!a.equals(b))
			changes.add(new ElasticSearchChangeRecord(nuc, null, fieldname, JaxbUtilModel.format(b),
			                                          JaxbUtilModel.format(a)));

		return changes;
	}

	private List<ElasticSearchChangeRecord> compareIsoDetails(IsoDetails a, IsoDetails b)
	{
		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (a == null && b == null)
			return changes;

		if (a == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoDetails", JaxbUtilModel.format(b), null));
			return changes;
		}

		if (b == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoDetails", null, JaxbUtilModel.format(a)));
			return changes;
		}

		if (a.getExpiryTime() != b.getExpiryTime())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoExpiryTime", Integer.toString(b.getExpiryTime()),
			                                          Integer.toString(a.getExpiryTime())));

		if (a.getIllPort() != b.getIllPort())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoIllPort", Integer.toString(b.getIllPort()),
			                                          Integer.toString(a.getIllPort())));

		if (!compareStrings(a.getIllServer(), b.getIllServer()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoIllServer", b.getIllServer(), a.getIllServer()));

		if (!compareStrings(a.getIsoSymbol(), b.getIsoSymbol()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSymbol", b.getIsoSymbol(), a.getIsoSymbol()));

		changes.addAll(compareValueDescPair("isoRequestExpiryType", a.getRequestExpiryType(),
		                                    b.getRequestExpiryType()));

		if (a.isAlternativeDocumentDelivery() != b.isAlternativeDocumentDelivery())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoAlternativeDocumentDelivery",
			                                          Boolean.toString(b.isAlternativeDocumentDelivery()),
			                                          Boolean.toString(a.isAlternativeDocumentDelivery())));

		if (a.isSendRequesterInformation() != b.isSendRequesterInformation())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSendRequesterInformation",
			                                          Boolean.toString(b.isSendRequesterInformation()),
			                                          Boolean.toString(a.isSendRequesterInformation())));

		if (a.isSharedBarcodes() != b.isSharedBarcodes())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSharedBarcodes",
			                                          Boolean.toString(b.isSharedBarcodes()),
			                                          Boolean.toString(a.isSharedBarcodes())));

		return changes;
	}

	private boolean compareStrings(String a, String b)
	{
		if (a == null && b == null)
			return true;

		if (a == null || b == null)
			return false;

		return a.equals(b);
	}

	@Override
	public Partner makePartner(ElasticSearchPartner e)
	{
		Partner p = of.createPartner();

		p.setLink(config.get("linkBase").orElse(DEFAULT_LINK_BASE) + e.getNuc());

		PartnerDetails partnerDetails = of.createPartnerDetails();
		p.setPartnerDetails(partnerDetails);

		if (ElasticSearchSuspension.SUSPENDED.equals(e.getStatus()))
			partnerDetails.setStatus(Status.INACTIVE);
		else
			partnerDetails.setStatus(Status.ACTIVE);

		partnerDetails.setCode(e.getNuc());
		partnerDetails.setName(e.getName());

		ProfileDetails profileDetails = of.createProfileDetails();
		partnerDetails.setProfileDetails(profileDetails);

		profileDetails.setProfileType(ProfileType.ISO);

		RequestExpiryType requestExpiryType = of.createRequestExpiryType();
		requestExpiryType.setValue(config.get("isoRequestExpiryTypeValue").orElse("INTEREST_DATE"));
		requestExpiryType.setDesc(config.get("isoRequestExpiryTypeDesc").orElse("Expire by interest date"));

		IsoDetails isoDetails = of.createIsoDetails();
		profileDetails.setIsoDetails(isoDetails);

		isoDetails.setAlternativeDocumentDelivery(false);
		isoDetails.setIllServer(config.get("isoIllServer").orElse("nla.vdxhost.com"));
		isoDetails.setIllPort(Integer.parseInt(config.get("isoIllPort").orElse("1611")));
		isoDetails.setIsoSymbol(!e.getNuc().startsWith("NLNZ") ? "NLA:" + e.getNuc() : e.getNuc());
		isoDetails.setSendRequesterInformation(Boolean.valueOf(config.get("isoSendRequesterInformation")
		                                                             .orElse("false")));
		isoDetails.setSharedBarcodes(Boolean.valueOf(config.get("isoSharedBarcodes").orElse("true")));
		isoDetails.setRequestExpiryType(requestExpiryType);

		SystemType systemType = of.createPartnerDetailsSystemType();
		systemType.setValue(config.get("systemTypeValue").orElse("LADD"));
		systemType.setDesc(config.get("systemTypeDesc").orElse("LADD"));

		LocateProfile locateProfile = of.createPartnerDetailsLocateProfile();
		locateProfile.setValue(config.get("locateProfileValue").orElse("LADD"));
		locateProfile.setDesc(config.get("locateProfileDesc").orElse("LADD Locate Profile"));

		partnerDetails.setSystemType(systemType);
		partnerDetails.setAvgSupplyTime(Integer.parseInt(config.get("avgSupplyTime").orElse("4")));
		partnerDetails.setDeliveryDelay(Integer.parseInt(config.get("deliveryDelay").orElse("4")));
		partnerDetails.setCurrency(config.get("currency").orElse("AUD"));
		partnerDetails.setBorrowingSupported(Boolean.parseBoolean(config.get("borrowingSupported").orElse("true")));
		partnerDetails.setBorrowingWorkflow(config.get("borrowingWorkflow").orElse("LADD_Borrowing"));
		partnerDetails.setLendingSupported(Boolean.parseBoolean(config.get("lendingSupported").orElse("true")));
		partnerDetails.setLendingWorkflow(config.get("lendingWorkflow").orElse("LADD_Lending"));
		partnerDetails.setLocateProfile(locateProfile);
		partnerDetails.setHoldingCode(e.getNuc());

		ContactInfo contactInfo = of.createContactInfo();
		p.setContactInfo(contactInfo);

		// AddressTypes: billing, claim, order, payment, returns, shipping, ALL
		Addresses addresses = of.createAddresses();
		contactInfo.setAddresses(addresses);

		Map<String, Address> addressMap = new HashMap<String, Address>();
		Map<String, String> addressTypeMap = new HashMap<String, String>();

		for (ElasticSearchPartnerAddress a : e.getAddresses())
		{
			if (!"active".equals(a.getAddressStatus()))
				continue;

			if (!isValidAddress(a.getAddressDetail()))
				continue;

			String addressType = a.getAddressType();
			if (addressType == null)
				continue;

			String addressSameAs = addressSameAs(a.getAddressDetail()).orElse(addressType);
			addressTypeMap.put(addressType.toLowerCase(), addressSameAs.toLowerCase());
			if (addressType.equals(addressSameAs))
			{
				a.getAddressDetail().setPreferred(false);
				a.getAddressDetail().setAddressTypes(of.createAddressAddressTypes());
				addressMap.put(addressType.toLowerCase(), ObjectUtil.deepClone(a.getAddressDetail()));
			}
		}

		List<String> addressTypes = new ArrayList<String>();
		addressTypes.add("claim");
		addressTypes.add("order");
		addressTypes.add("returns");
		addressTypes.add("shipping");
		addressTypes.add("billing");
		addressTypes.add("payment");

		List<String> addressesList = new ArrayList<String>();
		if (addressTypeMap.keySet().contains("billing"))
			addressesList.add("billing");

		if (addressTypeMap.keySet().contains("postal"))
			addressesList.add("postal");

		if (addressTypeMap.keySet().contains("main"))
			addressesList.add("main");

		for (int x = 0; x < addressesList.size(); x++)
		{
			String addressType = addressesList.get(x);

			Address a = resolveAddress(addressTypeMap, addressMap, addressType);

			if (a != null)
			{
				switch (addressType)
				{
					case "billing":
						if (addressTypes.remove("billing"))
							a.getAddressTypes().getAddressType().add("billing");

						if (addressTypes.remove("payment"))
							a.getAddressTypes().getAddressType().add("payment");

						break;

					case "postal":
						if (addressTypes.remove("shipping"))
							a.getAddressTypes().getAddressType().add("shipping");

						if (addressTypes.remove("returns"))
							a.getAddressTypes().getAddressType().add("returns");

						break;

					case "main":
						if (addressTypes.remove("claim"))
							a.getAddressTypes().getAddressType().add("claim");

						if (addressTypes.remove("order"))
							a.getAddressTypes().getAddressType().add("order");

						break;
				}

				if (x == (addressesList.size() - 1))
					a.getAddressTypes().getAddressType().addAll(addressTypes);

				Collections.sort(a.getAddressTypes().getAddressType());

				if (addressType.equals(addressTypeMap.get(addressType)))
					addresses.getAddress().add(a);
			}
		}

		// set preferred
		String preferredAddressType = config.get("preferredAddressType").orElse("").toLowerCase();
		boolean preferredAddressTypeSet = false;
		for (Address address : addresses.getAddress())
			if (!preferredAddressTypeSet && address.getAddressTypes().getAddressType().contains(preferredAddressType))
			{
				address.setPreferred(true);
				preferredAddressTypeSet = true;
			}

		if (!preferredAddressTypeSet)
			for (Address address : addresses.getAddress())
				if (address.getAddressTypes().getAddressType().contains("ALL"))
				{
					address.setPreferred(true);
					break;
				}

		// PhoneTypes: claimFax, orderFax, paymentFax, returnsFax,
		// claimPhone, orderPhone, paymentPhone, returnsPhone
		Phones phones = of.createPhones();
		contactInfo.setPhones(phones);

		String preferredPhoneType = config.get("preferredPhoneType").orElse("");
		boolean preferredPhoneTypeSet = false;

		if (e.getPhoneIll() != null && !"".equals(e.getPhoneIll()))
		{
			String phoneTmp = e.getPhoneIll();
			if (phoneTmp.length() > 40)
				phoneTmp = phoneTmp.substring(0, 40);

			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("orderPhone");
			phoneTypes.getPhoneType().add("claimPhone");
			phoneTypes.getPhoneType().add("paymentPhone");
			phoneTypes.getPhoneType().add("returnsPhone");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(phoneTmp);
			phone.setPreferred(false);
			if (!preferredPhoneTypeSet && phoneTypes.getPhoneType().contains(preferredPhoneType))
			{
				phone.setPreferred(true);
				preferredPhoneTypeSet = true;
			}

			Collections.sort(phone.getPhoneTypes().getPhoneType());

			phones.getPhone().add(phone);
		}

		if (e.getPhoneFax() != null && !"".equals(e.getPhoneFax()))
		{
			String phoneTmp = e.getPhoneFax();
			if (phoneTmp.length() > 40)
				phoneTmp = phoneTmp.substring(0, 40);

			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("orderFax");
			phoneTypes.getPhoneType().add("claimFax");
			phoneTypes.getPhoneType().add("paymentFax");
			phoneTypes.getPhoneType().add("returnsFax");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(phoneTmp);
			phone.setPreferred(false);
			if (!preferredPhoneTypeSet && phoneTypes.getPhoneType().contains(preferredPhoneType))
			{
				phone.setPreferred(true);
				preferredPhoneTypeSet = true;
			}

			Collections.sort(phone.getPhoneTypes().getPhoneType());

			phones.getPhone().add(phone);
		}

		if (e.getPhoneMain() != null && !"".equals(e.getPhoneMain()))
		{
			String phoneTmp = e.getPhoneMain();
			if (phoneTmp.length() > 40)
				phoneTmp = phoneTmp.substring(0, 40);

			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("orderPhone");
			phoneTypes.getPhoneType().add("claimPhone");
			phoneTypes.getPhoneType().add("paymentPhone");
			phoneTypes.getPhoneType().add("returnsPhone");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(phoneTmp);
			phone.setPreferred(false);
			if (!preferredPhoneTypeSet && phoneTypes.getPhoneType().contains(preferredPhoneType))
			{
				phone.setPreferred(true);
				preferredPhoneTypeSet = true;
			}

			Collections.sort(phone.getPhoneTypes().getPhoneType());

			phones.getPhone().add(phone);
		}

		// EmailType: claimMail, orderMail, paymentMail, queries, returnsMail
		Emails emails = of.createEmails();
		contactInfo.setEmails(emails);

		String preferredEmailType = config.get("preferredEmailType").orElse("");
		boolean preferredEmailTypeSet = false;

		if (e.getEmailIll() != null && !"".equals(e.getEmailIll()))
		{
			EmailTypes emailTypes = of.createEmailEmailTypes();
			emailTypes.getEmailType().add("claimMail");
			emailTypes.getEmailType().add("orderMail");
			emailTypes.getEmailType().add("paymentMail");
			emailTypes.getEmailType().add("queriesMail");
			emailTypes.getEmailType().add("returnsMail");

			Email email = of.createEmail();
			email.setEmailTypes(emailTypes);
			email.setEmailAddress(e.getEmailIll());
			email.setPreferred(false);
			if (!preferredEmailTypeSet && emailTypes.getEmailType().contains(preferredEmailType))
			{
				email.setPreferred(true);
				preferredEmailTypeSet = true;
			}

			Collections.sort(email.getEmailTypes().getEmailType());

			emails.getEmail().add(email);
		}

		if (e.getEmailMain() != null && !"".equals(e.getEmailMain()))
		{
			EmailTypes emailTypes = of.createEmailEmailTypes();
			emailTypes.getEmailType().add("claimMail");
			emailTypes.getEmailType().add("orderMail");
			emailTypes.getEmailType().add("paymentMail");
			emailTypes.getEmailType().add("queriesMail");
			emailTypes.getEmailType().add("returnsMail");

			Email email = of.createEmail();
			email.setEmailTypes(emailTypes);
			email.setEmailAddress(e.getEmailMain());
			email.setPreferred(false);
			if (!preferredEmailTypeSet &&
			    (emailTypes.getEmailType().contains(preferredEmailType) || "ALL".equalsIgnoreCase(preferredEmailType)))
			{
				email.setPreferred(true);
				preferredEmailTypeSet = true;
			}

			Collections.sort(email.getEmailTypes().getEmailType());

			emails.getEmail().add(email);
		}

		return p;
	}

	private Optional<String> addressSameAs(Address a)
	{
		if (a.getLine1() != null && a.getLine1().toLowerCase().startsWith("same as"))
		{
			Matcher m = patternSameAs.matcher(a.getLine1());
			if (m.find())
				return Optional.of(m.group(1).toLowerCase());
		}
		else if (a.getLine2() != null && a.getLine2().toLowerCase().startsWith("same as"))
		{
			Matcher m = patternSameAs.matcher(a.getLine2());
			if (m.find())
				return Optional.of(m.group(1).toLowerCase());
		}
		else if (a.getLine3() != null && a.getLine3().toLowerCase().startsWith("same as"))
		{
			Matcher m = patternSameAs.matcher(a.getLine3());
			if (m.find())
				return Optional.of(m.group(1).toLowerCase());
		}

		return Optional.empty();
	}

	private Address resolveAddress(Map<String, String> addressTypeMap, Map<String, Address> addresses,
	                               String addressType)
	{
		String a = addressTypeMap.get(addressType);
		while (a != addressTypeMap.get(a))
			a = addressTypeMap.get(a);

		return addresses.get(a);
	}

	private boolean isValidAddress(Address a)
	{
		if (a.getLine1() == null)
			return false;

		if (a.getCity() == null && !((a.getLine1() != null && a.getLine1().toLowerCase().startsWith("same as")) ||
		                             (a.getLine2() != null && a.getLine2().toLowerCase().startsWith("same as")) ||
		                             (a.getLine3() != null && a.getLine3().toLowerCase().startsWith("same as"))))
			return false;

		return true;
	}
}
