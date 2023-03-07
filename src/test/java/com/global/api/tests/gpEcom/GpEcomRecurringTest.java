package com.global.api.tests.gpEcom;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Customer;
import com.global.api.entities.StoredCredential;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.DccProcessor;
import com.global.api.entities.enums.DccRateType;
import com.global.api.entities.enums.RecurringSequence;
import com.global.api.entities.enums.RecurringType;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.entities.exceptions.UnsupportedTransactionException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.paymentMethods.RecurringPaymentMethod;
import com.global.api.serviceConfigs.GpEcomConfig;
import org.joda.time.DateTime;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GpEcomRecurringTest {
    private Customer new_customer;
    private CreditCardData card;

    private String customerId() {
        return String.format("%s-GlobalApi", new SimpleDateFormat("yyyyMMdd").format(new Date()));
    }
    private String paymentId(String type) {
        return String.format("%s-GlobalApi-%s", new SimpleDateFormat("yyyyMMdd").format(new Date()), type);
    }

    public GpEcomRecurringTest() throws ApiException {
        GpEcomConfig config = new GpEcomConfig();
        config.setMerchantId("heartlandgpsandbox");
        config.setAccountId("api");
        config.setSharedSecret("secret");
        config.setRefundPassword("refund");
        config.setServiceUrl("https://api.sandbox.realexpayments.com/epage-remote.cgi");

        ServicesContainer.configureService(config, "test");
        
        Address address = new Address();
        address.setStreetAddress1("Flat 123");
        address.setStreetAddress2("House 456");
        address.setStreetAddress3("The Cul-De-Sac");
        address.setCity("Halifax");
        address.setProvince("West Yorkshire");
        address.setPostalCode("W6 9HR");
        address.setCountry("United Kingdom");

        new_customer = new Customer();
        new_customer.setKey(customerId());
        new_customer.setTitle("Mr.");
        new_customer.setFirstName("James");
        new_customer.setLastName("Mason");
        new_customer.setCompany("Realex Payments");
        new_customer.setAddress(address);
        new_customer.setHomePhone("+35312345678");
        new_customer.setWorkPhone("+3531987654321");
        new_customer.setFax("+124546871258");
        new_customer.setMobilePhone("+25544778544");
        new_customer.setEmail("text@example.com");
        new_customer.setComments("Campaign Ref E7373G");

        card = new CreditCardData();
        card.setNumber("4263970000005262");
        card.setExpMonth(5);
        card.setExpYear(DateTime.now().getYear() + 2);
        card.setCardHolderName("James Mason");
    }

    @Test
    public void Test_001a_CreateCustomer() throws ApiException {
        try {
            Customer customer = new_customer.create("test");
            assertNotNull(customer);
        }
        catch (GatewayException exc) {
            // check for already created
            if (!exc.getResponseCode().equals("501"))
                throw exc;
        }
    }

    @Test
    public void Test_001b_CreatePaymentMethod() throws ApiException {
        try {
            RecurringPaymentMethod paymentMethod = new_customer.addPaymentMethod(paymentId("Credit"), card).create("test");
            assertNotNull(paymentMethod);
        }
        catch (GatewayException exc) {
            // check for already created
            if(!exc.getResponseCode().equals("520"))
                throw exc;
        }
    }

    @Test
    public void Test_001c_CreatePaymentMethodWithStoredCredential() throws ApiException {
        try {
            StoredCredential storedCredential = new StoredCredential();
            storedCredential.setSchemeId("YOUR_DESIRED_SCHEME_ID");

            RecurringPaymentMethod paymentMethod =
                    new_customer
                            .addPaymentMethod(paymentId("Credit") + UUID.randomUUID().toString().substring(0, 5), card, storedCredential)
                            .create("test");

            assertNotNull(paymentMethod);
        }
        catch (GatewayException exc) {
            // check for already created
            if(!exc.getResponseCode().equals("520"))
                throw exc;
        }
    }

    @Test
    public void Test_002a_EditCustomer() throws ApiException {
        Customer customer = new Customer();
        customer.setKey(customerId());
        customer.setFirstName("Perry");
        customer.saveChanges("test");
    }

    @Test
    public void Test_002b_EditPaymentMethod() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        CreditCardData newCard = new CreditCardData();
        newCard.setNumber("5425230000004415");
        newCard.setExpMonth(10);
        newCard.setExpYear(2025);
        newCard.setCardHolderName("Philip Marlowe");

        paymentMethod.setPaymentMethod(newCard);
        paymentMethod.saveChanges("test");
    }

    @Test
    public void Test_002c_EditPaymentMethodWithStoredCredential() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        CreditCardData newCard = new CreditCardData();
        newCard.setNumber("5425230000004415");
        newCard.setExpMonth(10);
        newCard.setExpYear(2025);
        newCard.setCardHolderName("Philip Marlowe");

        paymentMethod.setPaymentMethod(newCard);
        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setSchemeId("YOUR_DESIRED_SCHEME_ID");
        paymentMethod.setStoredCredential(storedCredential);
        paymentMethod.saveChanges("test");
    }

    @Test
    public void Test_002c_EditPaymentMethodExpOnly() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        CreditCardData card = new CreditCardData();
        card.setCardType("MC");
        card.setExpMonth(10);
        card.setExpYear(2025);
        card.setCardHolderName("Philip Marlowe");

        paymentMethod.setPaymentMethod(card);
        paymentMethod.saveChanges("test");
    }

    @Test(expected = UnsupportedTransactionException.class)
    public void Test_003_FindOnRealex() throws ApiException {
        Customer.find(customerId(), "test");
    }

    @Test
    public void Test_004a_ChargeStoredCard() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        Transaction response = paymentMethod.charge(new BigDecimal("10"))
                .withCurrency("USD")
                .withCvn("123")
                .execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
    }

    @Test
    public void Test_004b_VerifyStoredCard() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        Transaction response = paymentMethod.verify()
                .withCvn("123")
                .execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
    }

    @Test
    public void Test_004c_RefundStoredCard() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        Transaction response = paymentMethod.refund(new BigDecimal("10.01"))
                .withCurrency("USD")
                .execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
    }

    @Test
    public void Test_005_RecurringPayment() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        Transaction response = paymentMethod.charge(new BigDecimal("12"))
                .withRecurringInfo(RecurringType.Fixed, RecurringSequence.First)
                .withCurrency("USD")
                .execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
    }

    @Test
    public void Test_006_DeletePaymentMethod() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        paymentMethod.delete(false, "test");
    }

    // Negative Test Cases
    @Test(expected = ApiException.class)
    public void Test_007_EditPaymentMethod_Invalid_Name() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        CreditCardData newCard = new CreditCardData();
        newCard.setNumber("5425230000004415");
        newCard.setExpMonth(1000);
        newCard.setExpYear(2020);
        newCard.setCardHolderName(null);

        paymentMethod.setPaymentMethod(newCard);
        paymentMethod.saveChanges("test");
    }

    @Test(expected = ApiException.class)
    public void Test_008_EditPaymentMethod_Invalid_Card() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(customerId(), paymentId("Credit"));
        CreditCardData newCard = new CreditCardData();
        newCard.setNumber("542523");
        newCard.setExpMonth(1000);
        newCard.setExpYear(2020);
        newCard.setCardHolderName("Philip Marlowe");

        paymentMethod.setPaymentMethod(newCard);
        paymentMethod.saveChanges("test");
    }

    @Test(expected = ApiException.class)
	public void Test_009_DccRateLookup_AuthNotEnabledAccount() throws ApiException {
		RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(
                "038cb8bc-0289-48cf-a5ad-8bfbe54e204a",
                "fe1bb177-0a35-421c-9b0e-c7623712387c"
        );

		Transaction dccResponse = paymentMethod.getDccRate(DccRateType.Sale, DccProcessor.Fexco)
                .withAmount(new BigDecimal("10.01"))
                .withCurrency("EUR")
                .execute("test");

		assertNotNull(dccResponse);
		assertEquals("00", dccResponse.getResponseCode());

		Transaction response = paymentMethod.authorize(new BigDecimal("10.01"))
		        .withCurrency("EUR")
		        .withOrderId(dccResponse.getOrderId())
		        .withDccRateData(dccResponse.getDccRateData())
				.execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
	}

    @Test(expected = ApiException.class)
	public void Test_010_DccRateLookup_ChargeNotEnabledAccount() throws ApiException {
        RecurringPaymentMethod paymentMethod = new RecurringPaymentMethod(
                "038cb8bc-0289-48cf-a5ad-8bfbe54e204a",
                "fe1bb177-0a35-421c-9b0e-c7623712387c"
        );

        Transaction dccResponse = paymentMethod.getDccRate(DccRateType.Sale, DccProcessor.Fexco)
                .withAmount(new BigDecimal("10.01"))
                .withCurrency("EUR")
                .execute("test");

        assertNotNull(dccResponse);
        assertEquals("00", dccResponse.getResponseCode());

        Transaction response = paymentMethod.charge(new BigDecimal("10.01"))
                .withCurrency("EUR")
                .withOrderId(dccResponse.getOrderId())
                .withDccRateData(dccResponse.getDccRateData())
                .execute("test");

        assertNotNull(response);
        assertEquals("00", response.getResponseCode());
	}
}