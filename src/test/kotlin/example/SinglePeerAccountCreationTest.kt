package example

import iroha.protocol.Endpoint.TxStatus
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import mu.KLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SinglePeerAccountCreationTest {

    val iroha = IrohaContainer()

    @BeforeAll
    fun beforeAll() {
        iroha.start()
    }

    @AfterAll
    fun afterAll() {
        iroha.stop()
    }

    @Test
    fun testWithIroha() {
        val api = iroha.api
        val transactions = AccountCreatorHelper.getTransactions("poorguyfromapoorfamily")
        api.transactionListSync(transactions)
        Thread.sleep(5000)
        transactions.forEach {
            val hash = Utils.hash(it)
            val txStatusSync = api.txStatusSync(hash)
            println(txStatusSync)
            assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, txStatusSync.txStatus)
        }
    }

    companion object : KLogging()
}
