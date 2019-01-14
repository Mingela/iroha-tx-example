package example

import iroha.protocol.Endpoint.TxStatus
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import mu.KLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotEquals

class SinglePeerAccountCreationTest {

    val iroha = IrohaContainer()

    @BeforeEach
    fun beforeAll() {
        iroha.start()
    }

    @AfterEach
    fun afterAll() {
        iroha.stop()
    }

    @Test
    fun testRolesWithIroha() {
        val api = iroha.api
        val transactions = AccountCreatorHelper.getRolesTransactions("poorguyfromapoorfamily")
        api.transactionListSync(transactions)
        Thread.sleep(5000)
        transactions.forEach {
            val hash = Utils.hash(it)
            val txStatusSync = api.txStatusSync(hash)
            println(txStatusSync)
            assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, txStatusSync.txStatus)
        }
        println(api.query(AccountCreatorHelper.getQuery("poorguyfromapoorfamily")))
    }

    @Test
    fun testBatchWithIroha() {
        val api = iroha.api
        val transactions = AccountCreatorHelper.getBatchTransactions("anotherguysufferingiroha")
        api.transactionListSync(transactions)
        Thread.sleep(5000)
        transactions.forEach {
            val hash = Utils.hash(it)
            val txStatusSync = api.txStatusSync(hash)
            println(txStatusSync)
            assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, txStatusSync.txStatus)
        }
        println(api.query(AccountCreatorHelper.getQuery("anotherguysufferingiroha")))
    }

    companion object : KLogging()
}
