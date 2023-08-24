package com.global.api.tests.network.nws;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.EntryMethod;
import com.global.api.entities.enums.Target;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.network.entities.NtsData;
import com.global.api.network.entities.PriorMessageInformation;
import com.global.api.network.enums.*;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.paymentMethods.CreditTrackData;
import com.global.api.serviceConfigs.AcceptorConfig;
import com.global.api.serviceConfigs.NetworkGatewayConfig;
import com.global.api.tests.BatchProvider;
import com.global.api.tests.StanGenerator;
import com.global.api.tests.testdata.TestCards;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NWSTokenizationTest {
    private CreditCardData card;
    private CreditTrackData track;

    public NWSTokenizationTest() throws ApiException {

        Address address = new Address();
        address.setName("My STORE");
        address.setStreetAddress1("1 MY STREET");
        address.setCity("MYTOWN");
        address.setPostalCode("90210");
        address.setState("KY");
        address.setCountry("USA");

        AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.setAddress(address);

        // data code values
        acceptorConfig.setCardDataInputCapability(CardDataInputCapability.ContactlessEmv_ContactEmv_MagStripe_KeyEntry);
        acceptorConfig.setCardHolderAuthenticationCapability(CardHolderAuthenticationCapability.PIN);
        acceptorConfig.setTerminalOutputCapability(TerminalOutputCapability.Printing_Display);

        // hardware software config values
        acceptorConfig.setHardwareLevel("34");
        acceptorConfig.setSoftwareLevel("21205710");

        // pos configuration values
        acceptorConfig.setSupportsPartialApproval(true);
        acceptorConfig.setSupportsShutOffAmount(true);
        acceptorConfig.setSupportsReturnBalance(true);
        acceptorConfig.setSupportsDiscoverNetworkReferenceId(true);
        acceptorConfig.setSupportsAvsCnvVoidReferrals(true);
        acceptorConfig.setSupportsEmvPin(true);

        //DE 127
        acceptorConfig.setServiceType(ServiceType.GPN_API);
        acceptorConfig.setTokenizationOperationType(TokenizationOperationType.DeTokenize);
        acceptorConfig.setTokenizationType(TokenizationType.MerchantTokenization);
        acceptorConfig.setMerchantId("650000011573667");

        //DE 127
        acceptorConfig.setSupportedEncryptionType(EncryptionType.TDES);
        acceptorConfig.setServiceType(ServiceType.GPN_API);
        acceptorConfig.setOperationType(OperationType.Decrypt);

        //card num 165473500000000014

        // gateway config
        NetworkGatewayConfig config = new NetworkGatewayConfig(Target.NWS);
        config.setPrimaryEndpoint("test.txns-c.secureexchange.net");
        config.setPrimaryPort(15031);
        config.setSecondaryEndpoint("test.txns-e.secureexchange.net");
        config.setSecondaryPort(15031);
        config.setCompanyId("SPSA");
        config.setTerminalId("NWSJAVA05");
        config.setUniqueDeviceId("0001");
        config.setAcceptorConfig(acceptorConfig);
        config.setEnableLogging(true);
        config.setStanProvider(StanGenerator.getInstance());
        config.setBatchProvider(BatchProvider.getInstance());
       // config.setMerchantType("5541");

        ServicesContainer.configureService(config);

//        card= TestCards.MasterCardManual();
//        card = TestCards.MasterCardPurchasingManual();
//        card = TestCards.DiscoverManual();
//        card = TestCards.VisaManual();
//        card = TestCards.VisaPurchasingManual();
//        card = TestCards.VisaCorporateManual();
//        card = TestCards.AmexManual();
//        card = TestCards.JcbManual();
//        card = TestCards.Unionpaymanual();
//        card = TestCards.PaypalManual();
    }

    @Test
    public void test_file_action() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("5473500000000014");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void() throws ApiException {
        card = TestCards.MasterCardManual();
        card.setTokenizationData("8E4BDE85FCF1FD72A6CC9A8AC0EB740A");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }

    //mastercard purchasing
    @Test
    public void test_file_action_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("5302490000004066");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_mastercard_purchasing() throws ApiException {
        card = TestCards.MasterCardPurchasingManual();
        card.setTokenizationData("4B76646C9A22E481DFB94CF1314E9301");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }


    //Visa Manual
    @Test
    public void test_file_action_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("4012002000060016");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_visa() throws ApiException {
        card = TestCards.VisaManual();
        card.setTokenizationData("FBFE7A3F3AD34F8211E556327CA5E379");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }

    //Visa Manual
    @Test
    public void test_file_action_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("4012002000060016");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_visa_purchasing() throws ApiException {
        card = TestCards.VisaPurchasingManual();
        card.setTokenizationData("E099BF9FBFEA0A06FF7B7779241CAFDB");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }

    //visa corporate
    @Test
    public void test_file_action_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("4013872718148777");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_visa_corporate() throws ApiException {
        card = TestCards.VisaCorporateManual();
        card.setTokenizationData("5F052EB94571A12965D2D6343525E9CA");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }


    //discover
    @Test
    public void test_file_action_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("6550006599174230");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
        response.getTransactionReference().setAuthCode("00479A");

        Transaction captureResponse = response.capture(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_discover() throws ApiException {
        card = TestCards.DiscoverManual();
        card.setTokenizationData("4D6B025705ADA3BC92392CB12D4C5A9E");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }
    //Amex

    @Test
    public void test_file_action_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("372700699251018");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_amex() throws ApiException {
        card = TestCards.AmexManual();
        card.setTokenizationData("4CCD57AAFF5477B986563BE1E70690B3");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }
    //JCB
    @Test
    public void test_file_action_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("3566007770007321");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_jcb() throws ApiException {
        card = TestCards.JcbManual();
        card.setTokenizationData("1DD5C11868C9717809EDD0BB12ACF61C");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }
    //Union Pay
    @Test
    public void test_file_action_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("6221260012345674");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_union_pay() throws ApiException {
        card = TestCards.UnionPayManual();
        card.setTokenizationData("F1C96E421546E54F77CB74EDFCDDCE65");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }

    //Paypal
    @Test
    public void test_file_action_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("6506001000010029");
        Transaction response = card.fileAction()
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    @Test
    public void test_001_credit_manual_auth_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");
        Transaction response = card.authorize(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());
    }
    @Test
    public void test_002_credit_manual_sale_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

    }
    //force draft capture
    @Test
    public void test_016_authCapture_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");
        Transaction response = card.authorize(new BigDecimal(10), true)
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("101", pmi.getFunctionCode());

        // check response
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        Transaction captureResponse = response.capture(response.getAuthorizedAmount())
                .withCurrency("USD")
                .execute();
        assertNotNull(captureResponse);

        // check message data
        pmi = captureResponse.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1220", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("1376", pmi.getMessageReasonCode());
        assertEquals("201", pmi.getFunctionCode());

        // check response
        assertEquals("000", captureResponse.getResponseCode());
    }
    @Test
    public void test_004_credit_refund_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");
        Transaction response = card.refund(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals("000", response.getResponseCode());
    }

    @Test
    public void test_005_credit_balance_inquiry_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");
        Transaction response = card.balanceInquiry()
                .execute();
        assertNotNull(response);

        PriorMessageInformation pmi = response.getMessageInformation();
        assertEquals("1100", pmi.getMessageTransactionIndicator());
        assertEquals("313000", pmi.getProcessingCode());
        assertEquals("108", pmi.getFunctionCode());

        assertEquals("000", response.getResponseCode());
    }
    @Test
    public void test_sale_reversal_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");

        NtsData ntsData = new NtsData(FallbackCode.Received_IssuerUnavailable,AuthorizerCode.Terminal_Authorized);
        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);

        response.setNtsData(ntsData);
        // check message data
        PriorMessageInformation pmi = response.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1200", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("200", pmi.getFunctionCode());

        // check response
        //assertEquals("000", response.getResponseCode());

        Transaction reversal = response.reverse(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(reversal);

        // check message data
        pmi = reversal.getMessageInformation();
        assertNotNull(pmi);
        assertEquals("1420", pmi.getMessageTransactionIndicator());
        assertEquals("003000", pmi.getProcessingCode());
        assertEquals("400", pmi.getFunctionCode());
        assertEquals("4021", pmi.getMessageReasonCode());

        // check response
        assertEquals("400", reversal.getResponseCode());
    }
    @Test
    public void test_015_credit_void_paypal() throws ApiException {
        card = TestCards.Paypal();
        card.setTokenizationData("77D2FFAEE984E740AC487E1E5A8E6726");

        Transaction response = card.charge(new BigDecimal(10))
                .withCurrency("USD")
                .execute();
        assertNotNull(response);
        assertEquals(response.getResponseMessage(), "000", response.getResponseCode());

        // reverse the transaction
        Transaction reverseResponse = response.voidTransaction().execute();
        assertNotNull(reverseResponse);
        assertEquals(response.getResponseMessage(), "400", reverseResponse.getResponseCode());
    }










}
