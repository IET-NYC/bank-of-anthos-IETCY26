/*
 * Copyright 2020, Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anthos.samples.bankofanthos.ledgerwriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static anthos.samples.bankofanthos.ledgerwriter.ExceptionMessages.
        EXCEPTION_MESSAGE_INVALID_NUMBER;
import static anthos.samples.bankofanthos.ledgerwriter.ExceptionMessages.
        EXCEPTION_MESSAGE_NOT_AUTHENTICATED;
import static anthos.samples.bankofanthos.ledgerwriter.ExceptionMessages.
        EXCEPTION_MESSAGE_SEND_TO_SELF;
import static anthos.samples.bankofanthos.ledgerwriter.ExceptionMessages.
        EXCEPTION_MESSAGE_INVALID_AMOUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class TransactionValidatorTest {

    private TransactionValidator transactionValidator;

    @Mock
    private Transaction transaction;

    private static final String LOCAL_ROUTING_NUM = "123456789";
    private static final String AUTHED_ACCOUNT_NUM = "1234567890";
    private static final String NON_AUTHED_ACCOUNT_NUM = "0987654321";
    private static final String TO_ACCOUNT_NUM = "5678901234";
    private static final String TO_ROUTING_NUM = "567891234";
    private static final String ZERO_ACCOUNT_NUM = "0000000000";
    private static final String ZERO_ROUTING_NUM = "000000000";
    private static final Integer VALID_AMOUNT = 3755;

    private static final String[] INVALID_ACCT_NUM = {
        null,
        "12345678",
        "123456789",
        "12345678900",
        "",
        "12345678j",
        "abcdefghij",
        "123 456789",
        "          ",
        "1234567.89",
        "123_456789",
        "12345仮6789",
        "12341545🐻"
    };

    private static final String[] INVALID_ROUTING_NUM = {
        null,
        "12345678",
        "1234567890",
        "12345678900",
        "",
        "12345678j",
        "abcdefghij",
        "123 456789",
        "          ",
        "1234567.89",
        "123_456789",
        "12345仮6789",
        "12341545🐻"
    };

    private static final Integer[] VALID_TRANSACTION_AMOUNT = {
        1, VALID_AMOUNT, Integer.MAX_VALUE
    };

    private static final Integer[] INVALID_TRANSACTION_AMOUNT = {
        null, 0, -1, -23, Integer.MIN_VALUE
    };

    @BeforeEach
    void setUp() {
        openMocks(this);
        transactionValidator = new TransactionValidator();

        when(transaction.getFromAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getToAccountNum()).thenReturn(TO_ACCOUNT_NUM);
        when(transaction.getToRoutingNum()).thenReturn(TO_ROUTING_NUM);
        when(transaction.getAmount()).thenReturn(VALID_AMOUNT);
    }

    @Test
    @DisplayName("Given the transaction is validated and amount is valid, no exception is thrown")
    void validateTransactionSuccess() {
        for (int i = 0; i < VALID_TRANSACTION_AMOUNT.length; i++) {
            // Given
            when(transaction.getAmount()).thenReturn(VALID_TRANSACTION_AMOUNT[i]);

            // Then
            assertDoesNotThrow(() -> {
                transactionValidator.validateTransaction(
                    LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
            });
        }
    }

    @Test
    @DisplayName("Given the sender is external and not authenticated, no exception is thrown")
    void validateTransactionSuccessWhenExternalSenderIsNotAuthenticated() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(TO_ROUTING_NUM);

        // Then
        assertDoesNotThrow(() -> {
            transactionValidator.validateTransaction(
                    LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
        });
    }

    @Test
    @DisplayName("Given sender and receiver accounts match but routing numbers differ, " +
            "no exception is thrown")
    void validateTransactionSuccessWhenAccountsMatchAcrossRoutingNumbers() {
        // Given
        when(transaction.getToAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);

        // Then
        assertDoesNotThrow(() -> {
            transactionValidator.validateTransaction(
                    LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
        });
    }

    @Test
    @DisplayName("Given an external sender and local receiver share an account number, " +
            "no exception is thrown")
    void validateTransactionSuccessWhenExternalSenderMatchesLocalReceiverAccount() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(TO_ROUTING_NUM);
        when(transaction.getToAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getToRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);

        // Then
        assertDoesNotThrow(() -> {
            transactionValidator.validateTransaction(
                    LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
        });
    }

    @Test
    @DisplayName("Given internal routing numbers match but accounts differ, " +
            "no exception is thrown")
    void validateTransactionSuccessWhenInternalRoutingMatchesAcrossAccounts() {
        // Given
        when(transaction.getToRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);

        // Then
        assertDoesNotThrow(() -> {
            transactionValidator.validateTransaction(
                    LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
        });
    }

    @Test
    @DisplayName("Given invalid sender account number, " +
            "IllegalArgumentException is thrown")
    void validationFail_WhenSenderAccountNumber_IsInvalid() {
        for (int i = 0; i < INVALID_ACCT_NUM.length; i++) {
            // Given
            when(transaction.getFromAccountNum()).thenReturn(INVALID_ACCT_NUM[i]);

            // When, Then
            assertInvalidNumberHelper();
        }
    }

    @Test
    @DisplayName("Given invalid sender routing number, " +
            "IllegalArgumentException is thrown")
    void validationFail_WhenSenderRoutingNumber_IsInvalid() {
        for (int i = 0; i < INVALID_ROUTING_NUM.length; i++) {
            // Given
            when(transaction.getFromRoutingNum()).thenReturn(INVALID_ROUTING_NUM[i]);

            // When, Then
            assertInvalidNumberHelper();
        }
    }

    @Test
    @DisplayName("Given invalid receiver account number, " +
        "IllegalArgumentException is thrown")
    void validationFail_WhenReceiverAccountNumber_IsValid() {
        for (int i = 0; i < INVALID_ACCT_NUM.length; i++) {
            // Given
            when(transaction.getToAccountNum()).thenReturn(INVALID_ACCT_NUM[i]);

            // When, Then
            assertInvalidNumberHelper();
        }
    }

    @Test
    @DisplayName("Given invalid receiver routing number, " +
        "IllegalArgumentException is thrown")
    void validationFail_WhenReceiverRoutingNumber_IsValid() {
        for (int i = 0; i < INVALID_ROUTING_NUM.length; i++) {
            // Given
            when(transaction.getToRoutingNum()).thenReturn(INVALID_ROUTING_NUM[i]);

            // When, Then
            assertInvalidNumberHelper();
        }
    }

    @Test
    @DisplayName("Given the sender is not authenticated, " +
            "IllegalArgumentException is thrown")
    void validateTransactionFailWhenNotAuthenticated() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_NOT_AUTHENTICATED,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given the sender is the receiver, " +
            "IllegalArgumentException is thrown")
    void validateTransactionFailWhenSenderIsReceiver() {
        // Given
        when(transaction.getToAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getToRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_SEND_TO_SELF,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given transaction amount is invalid, IllegalArgumentException is thrown")
    void validationFail_WhenTransactionAmount_IsInvalid() {
        for (int i = 0; i < INVALID_TRANSACTION_AMOUNT.length; i++) {
            // Given
            when(transaction.getAmount()).thenReturn(INVALID_TRANSACTION_AMOUNT[i]);

            // When
            IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                        LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

            // Then
            assertNotNull(exceptionThrown);
            assertEquals(EXCEPTION_MESSAGE_INVALID_AMOUNT,
                exceptionThrown.getMessage());
        }
    }

    @Test
    @DisplayName("Given an external sender sends to the same external account, "
            + "IllegalArgumentException is thrown")
    void validateTransactionFailWhenExternalSenderIsExternalReceiver() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(TO_ROUTING_NUM);
        when(transaction.getToAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_SEND_TO_SELF,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given a malformed account number on an unauthenticated "
            + "sender, the invalid number error takes precedence")
    void validateTransactionFailWithInvalidNumberBeforeAuthenticationCheck() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn("abcdefghij");

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_INVALID_NUMBER,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given an unauthenticated internal sender sends to itself, "
            + "the authentication error takes precedence")
    void validateTransactionFailWithAuthenticationBeforeSendToSelfCheck() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getToAccountNum()).thenReturn(
                NON_AUTHED_ACCOUNT_NUM);
        when(transaction.getToRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_NOT_AUTHENTICATED,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given a self transfer with a non-positive amount, "
            + "the send to self error takes precedence")
    void validateTransactionFailWithSendToSelfBeforeAmountCheck() {
        // Given
        when(transaction.getToAccountNum()).thenReturn(AUTHED_ACCOUNT_NUM);
        when(transaction.getToRoutingNum()).thenReturn(LOCAL_ROUTING_NUM);
        when(transaction.getAmount()).thenReturn(0);

        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_SEND_TO_SELF,
                exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("Given account and routing numbers padded with leading zeros, "
            + "no exception is thrown")
    void validateTransactionSuccessWhenNumbersHaveLeadingZeros() {
        // Given
        when(transaction.getFromAccountNum()).thenReturn(ZERO_ACCOUNT_NUM);
        when(transaction.getFromRoutingNum()).thenReturn(ZERO_ROUTING_NUM);
        when(transaction.getToRoutingNum()).thenReturn(ZERO_ROUTING_NUM);

        // Then
        assertDoesNotThrow(() -> {
            transactionValidator.validateTransaction(
                    ZERO_ROUTING_NUM, ZERO_ACCOUNT_NUM, transaction);
        });
    }

    void assertInvalidNumberHelper() {
        // When
        IllegalArgumentException exceptionThrown = assertThrows(
                IllegalArgumentException.class, () -> {
                    transactionValidator.validateTransaction(
                            LOCAL_ROUTING_NUM, AUTHED_ACCOUNT_NUM, transaction);
                });

        // Then
        assertNotNull(exceptionThrown);
        assertEquals(EXCEPTION_MESSAGE_INVALID_NUMBER,
                exceptionThrown.getMessage());
    }
}
