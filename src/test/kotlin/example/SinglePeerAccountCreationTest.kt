package example

import iroha.protocol.Endpoint.TxStatus
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import mu.KLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    }

    @Test
    fun testBatchWithIroha() {
        val api = iroha.api
        val transactions = Utils.createTxAtomicBatch(AccountCreatorHelper.getBatchTransactions("anotherguysufferingiroha"), GenesisBlockBuilder.defaultKeyPair)
        api.transactionListSync(transactions)
        Thread.sleep(5000)
        transactions.forEach {
            val hash = Utils.hash(it)
            val txStatusSync = api.txStatusSync(hash)
            println(txStatusSync)
            assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, txStatusSync.txStatus)
        }
    }

    @Test
    fun testStatusStream() {
        val api = iroha.api
        val transactions = AccountCreatorHelper.getBatchTransactions("onemorepoorman")
        api.transactionListSync(transactions)
        transactions.map { Utils.hash(it) }.forEach {
            api.txStatus(it).blockingSubscribe { statusResponse ->
                println(statusResponse)
                assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, statusResponse.txStatus)
            }
        }
    }

    companion object : KLogging()
}
