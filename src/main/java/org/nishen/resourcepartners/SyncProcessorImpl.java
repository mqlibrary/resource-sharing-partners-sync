package org.nishen.resourcepartners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nishen.resourcepartners.dao.AlmaDAO;
import org.nishen.resourcepartners.dao.AlmaDAOFactory;
import org.nishen.resourcepartners.dao.Config;
import org.nishen.resourcepartners.dao.ConfigFactory;
import org.nishen.resourcepartners.dao.ElasticSearchDAO;
import org.nishen.resourcepartners.entity.ElasticSearchChangeRecord;
import org.nishen.resourcepartners.entity.ElasticSearchPartner;
import org.nishen.resourcepartners.entity.ElasticSearchPartnerAddress;
import org.nishen.resourcepartners.entity.ElasticSearchSuspension;
import org.nishen.resourcepartners.model.Address;
import org.nishen.resourcepartners.model.Address.AddressTypes;
import org.nishen.resourcepartners.model.Addresses;
import org.nishen.resourcepartners.model.ContactInfo;
import org.nishen.resourcepartners.model.Email;
import org.nishen.resourcepartners.model.Email.EmailTypes;
import org.nishen.resourcepartners.model.Emails;
import org.nishen.resourcepartners.model.IsoDetails;
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
import org.nishen.resourcepartners.util.JaxbUtilModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class SyncProcessorImpl implements SyncProcessor
{
	private static final Logger log = LoggerFactory.getLogger(SyncProcessorImpl.class);

	private static final String DEFAULT_LINK_BASE = "https://api-ap.hosted.exlibrisgroup.com/almaws/v1/partners/";

	private Config config;

	private ElasticSearchDAO elastic;

	private AlmaDAO alma;

	private String nuc;

	private ObjectFactory of = new ObjectFactory();

	@Inject
	public SyncProcessorImpl(ElasticSearchDAO elastic, AlmaDAOFactory almaFactory, ConfigFactory configFactory,
	                         @Assisted("nuc") String nuc, @Assisted("apikey") String apikey)
	{
		this.config = configFactory.create(nuc);
		this.elastic = elastic;
		this.alma = almaFactory.create(apikey);
		this.nuc = nuc;

		log.debug("instantiated class: {}", this.getClass().getName());
	}

	@Override
	public Optional<Map<String, Partner>> sync() throws SyncException
	{
		log.debug("sync nuc: {}", nuc);

		Map<String, Partner> changed = new HashMap<String, Partner>();

		Map<String, ElasticSearchPartner> elasticPartners = elastic.getPartners();
		Map<String, Partner> almaPartners = alma.getPartners();

		Map<String, List<ElasticSearchChangeRecord>> allChanges =
		        new HashMap<String, List<ElasticSearchChangeRecord>>();

		List<String> remaining = new ArrayList<String>(almaPartners.keySet());

		for (String s : elasticPartners.keySet())
		{
			remaining.remove(s);

			Partner a = makePartner(elasticPartners.get(s));
			Partner b = almaPartners.get(s);

			// we keep notes from Alma - source of truth for notes.
			if (b != null)
				a.setNotes(b.getNotes());

			List<ElasticSearchChangeRecord> changes = comparePartners(a, b);
			if (changes.size() > 0)
			{
				// set the nuc (partner) that this change was made for
				for (ElasticSearchChangeRecord c : changes)
					c.setNuc(s);

				allChanges.put(s, changes);
				changed.put(s, a);
			}
		}

		return Optional.of(changed);
	}

	private List<ElasticSearchChangeRecord> comparePartners(Partner a, Partner b)
	{
		assert (a != null);

		List<ElasticSearchChangeRecord> changes = new ArrayList<ElasticSearchChangeRecord>();

		if (b == null)
		{
			changes.add(new ElasticSearchChangeRecord(nuc, null, "partner", null, JaxbUtilModel.format(a)));
			return changes;
		}

		if (compareStrings(a.getLink(), b.getLink()))
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
			                                          Integer.toString(a.getAvgSupplyTime()),
			                                          Integer.toString(b.getAvgSupplyTime())));

		if (a.isBorrowingSupported() != b.isBorrowingSupported())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "borrowingSupported",
			                                          Boolean.toString(a.isBorrowingSupported()),
			                                          Boolean.toString(a.isBorrowingSupported())));

		if (compareStrings(a.getBorrowingWorkflow(), b.getBorrowingWorkflow()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "borrowingWorkflow", a.getBorrowingWorkflow(),
			                                          b.getBorrowingWorkflow()));

		if (compareStrings(a.getCode(), b.getCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "code", a.getCode(), b.getCode()));

		if (compareStrings(a.getCurrency(), b.getCurrency()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "currency", a.getCurrency(), b.getCurrency()));

		if (a.getDeliveryDelay() != b.getDeliveryDelay())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "deliveryDelay",
			                                          Integer.toString(a.getDeliveryDelay()),
			                                          Integer.toString(b.getDeliveryDelay())));

		if (compareStrings(a.getHoldingCode(), b.getHoldingCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "holdingCode", a.getHoldingCode(),
			                                          b.getHoldingCode()));

		if (compareStrings(a.getInstitutionCode(), b.getInstitutionCode()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "institutionCode", a.getInstitutionCode(),
			                                          b.getInstitutionCode()));

		if (a.isLendingSupported() != b.isLendingSupported())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "lendingSupported",
			                                          Boolean.toString(a.isLendingSupported()),
			                                          Boolean.toString(a.isLendingSupported())));

		if (compareStrings(a.getLendingWorkflow(), b.getLendingWorkflow()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "lendingWorkflow", a.getLendingWorkflow(),
			                                          b.getLendingWorkflow()));

		changes.addAll(compareValueDescPair("locateProfile", a.getLocateProfile(), b.getLocateProfile()));

		if (compareStrings(a.getName(), b.getName()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "name", a.getName(), b.getName()));

		if (!a.getStatus().equals(b.getStatus()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "status", a.getStatus().toString(),
			                                          b.getStatus().toString()));

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
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoExpiryTime", Integer.toString(a.getExpiryTime()),
			                                          Integer.toString(b.getExpiryTime())));

		if (a.getIllPort() != b.getIllPort())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoIllPort", Integer.toString(a.getIllPort()),
			                                          Integer.toString(b.getIllPort())));

		if (compareStrings(a.getIllServer(), b.getIllServer()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoIllServer", a.getIllServer(), b.getIllServer()));

		if (compareStrings(a.getIsoSymbol(), b.getIsoSymbol()))
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSymbol", a.getIsoSymbol(), b.getIsoSymbol()));

		changes.addAll(compareValueDescPair("isoRequestExpiryType", a.getRequestExpiryType(),
		                                    b.getRequestExpiryType()));

		if (a.isAlternativeDocumentDelivery() != b.isAlternativeDocumentDelivery())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoAlternativeDocumentDelivery",
			                                          Boolean.toString(a.isAlternativeDocumentDelivery()),
			                                          Boolean.toString(a.isAlternativeDocumentDelivery())));

		if (a.isSendRequesterInformation() != b.isSendRequesterInformation())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSendRequesterInformation",
			                                          Boolean.toString(a.isSendRequesterInformation()),
			                                          Boolean.toString(a.isSendRequesterInformation())));

		if (a.isSharedBarcodes() != b.isSharedBarcodes())
			changes.add(new ElasticSearchChangeRecord(nuc, null, "isoSharedBarcodes",
			                                          Boolean.toString(a.isSharedBarcodes()),
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

	private Partner makePartner(ElasticSearchPartner e)
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
		requestExpiryType.setValue("INTEREST_DATE");
		requestExpiryType.setDesc("Expire by interest date");

		IsoDetails isoDetails = of.createIsoDetails();
		profileDetails.setIsoDetails(isoDetails);

		isoDetails.setAlternativeDocumentDelivery(false);
		isoDetails.setIllServer(config.get("isoIllServer").get());
		isoDetails.setIllPort(Integer.parseInt(config.get("isoIllPort").get()));
		isoDetails.setIsoSymbol(!e.getNuc().startsWith("NLNZ") ? "NLA:" + e.getNuc() : e.getNuc());
		isoDetails.setSendRequesterInformation(false);
		isoDetails.setSharedBarcodes(true);
		isoDetails.setRequestExpiryType(requestExpiryType);

		SystemType systemType = of.createPartnerDetailsSystemType();
		systemType.setValue("LADD");
		systemType.setDesc("LADD");

		LocateProfile locateProfile = of.createPartnerDetailsLocateProfile();
		locateProfile.setValue("LADD");
		locateProfile.setDesc("LADD Locate Profile");

		partnerDetails.setSystemType(systemType);
		partnerDetails.setAvgSupplyTime(4);
		partnerDetails.setDeliveryDelay(4);
		partnerDetails.setCurrency("AUD");
		partnerDetails.setBorrowingSupported(true);
		partnerDetails.setBorrowingWorkflow("LADD_Borrowing");
		partnerDetails.setLendingSupported(true);
		partnerDetails.setLendingWorkflow("LADD_Lending");
		partnerDetails.setLocateProfile(locateProfile);
		partnerDetails.setHoldingCode(e.getNuc());

		ContactInfo contactInfo = of.createContactInfo();
		p.setContactInfo(contactInfo);

		Addresses addresses = of.createAddresses();
		contactInfo.setAddresses(addresses);

		for (ElasticSearchPartnerAddress a : e.getAddresses())
		{
			if (!"active".equals(a.getAddressStatus()))
				continue;

			String addressType = a.getAddressType();
			if ("billing".equals(addressType))
				addressType = "billing";
			else if ("postal".equals(addressType))
				addressType = "shipping";
			else
				addressType = "ALL";

			AddressTypes addressTypes = of.createAddressAddressTypes();
			addressTypes.getAddressType().add(addressType);

			a.getAddressDetail().setAddressTypes(addressTypes);
			addresses.getAddress().add(a.getAddressDetail());
		}

		Phones phones = of.createPhones();
		contactInfo.setPhones(phones);

		if (e.getPhoneMain() != null && !"".equals(e.getPhoneMain()))
		{
			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("ALL");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(e.getPhoneMain());

			phones.getPhone().add(phone);
		}

		if (e.getPhoneIll() != null && !"".equals(e.getPhoneIll()))
		{
			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("order_phone");
			phoneTypes.getPhoneType().add("claim_phone");
			phoneTypes.getPhoneType().add("payment_phone");
			phoneTypes.getPhoneType().add("returns_phone");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(e.getPhoneIll());

			phones.getPhone().add(phone);
		}

		if (e.getPhoneFax() != null && !"".equals(e.getPhoneFax()))
		{
			PhoneTypes phoneTypes = of.createPhonePhoneTypes();
			phoneTypes.getPhoneType().add("order_fax");
			phoneTypes.getPhoneType().add("claim_fax");
			phoneTypes.getPhoneType().add("payment_fax");
			phoneTypes.getPhoneType().add("returns_fax");

			Phone phone = of.createPhone();
			phone.setPhoneTypes(phoneTypes);
			phone.setPhoneNumber(e.getPhoneFax());

			phones.getPhone().add(phone);
		}

		Emails emails = of.createEmails();
		contactInfo.setEmails(emails);

		if (e.getEmailMain() != null && !"".equals(e.getEmailMain()))
		{
			EmailTypes emailTypes = of.createEmailEmailTypes();
			emailTypes.getEmailType().add("ALL");

			Email email = of.createEmail();
			email.setEmailTypes(emailTypes);
			email.setEmailAddress(e.getEmailMain());

			emails.getEmail().add(email);
		}

		if (e.getEmailIll() != null && !"".equals(e.getEmailIll()))
		{
			EmailTypes emailTypes = of.createEmailEmailTypes();
			emailTypes.getEmailType().add("ALL");

			Email email = of.createEmail();
			email.setEmailTypes(emailTypes);
			email.setEmailAddress(e.getEmailIll());

			emails.getEmail().add(email);
		}

		return p;
	}
}
