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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = "spring.jpa.database-platform=")
@ContextConfiguration(classes = TransactionPersistenceTest.TestApplication.class)
class TransactionPersistenceTest {

    private static final String TRANSACTION_JSON = """
        {
          "fromAccountNum": "1234567890",
          "fromRoutingNum": "123456789",
          "toAccountNum": "0987654321",
          "toRoutingNum": "987654321",
          "amount": 3755,
          "uuid": "request-id"
        }
        """;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    void persistsTransactionWithJakartaMapping() throws Exception {
        Transaction transaction = new ObjectMapper().readValue(
                TRANSACTION_JSON, Transaction.class);

        Transaction saved = transactionRepository.save(transaction);
        entityManager.flush();
        long transactionId = saved.getTransactionId();
        entityManager.clear();

        Transaction reloaded = transactionRepository.findById(transactionId)
                .orElseThrow();

        assertTrue(transactionId > 0);
        assertEquals(transaction.getFromAccountNum(),
                reloaded.getFromAccountNum());
        assertEquals(transaction.getFromRoutingNum(),
                reloaded.getFromRoutingNum());
        assertEquals(transaction.getToAccountNum(),
                reloaded.getToAccountNum());
        assertEquals(transaction.getToRoutingNum(),
                reloaded.getToRoutingNum());
        assertEquals(transaction.getAmount(), reloaded.getAmount());
        assertEquals("", reloaded.getRequestUuid());
    }
}
