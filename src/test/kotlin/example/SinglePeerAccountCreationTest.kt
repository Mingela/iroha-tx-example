package example

import iroha.protocol.Endpoint.TxStatus
import jp.co.soramitsu.iroha.java.BlocksQueryBuilder
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import mu.KLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class SinglePeerAccountCreationTest {

    private val iroha = IrohaContainer().withLogger(null)

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
        val transactions = Utils.createTxAtomicBatch(
            AccountCreatorHelper.getBatchTransactions("anotherguysufferingiroha"),
            GenesisBlockBuilder.defaultKeyPair
        )
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

    @Test
    fun worldStateViewUpdating() {
        val api = iroha.api
        val counter = AtomicLong(1)
        val query =
            BlocksQueryBuilder(GenesisBlockBuilder.defaultAccountId, Instant.now(), counter.getAndIncrement())
                .buildSigned(GenesisBlockBuilder.defaultKeyPair)
        api.blocksQuery(query).subscribe {
            val payload = it.blockResponse.block.blockV1.payload
            println("Got block #${payload.height}. Let's query the account")
            // try to play with this value
            Thread.sleep(200)
            val createAccount =
                payload.transactionsList[0].payload.reducedPayload.commandsList[0].createAccount
            val queryResponse = api.query(
                Query.builder(GenesisBlockBuilder.defaultAccountId, counter.getAndIncrement())
                    .getAccount(createAccount.accountName + "@" + createAccount.domainId)
                    .buildSigned(GenesisBlockBuilder.defaultKeyPair)
            )
            println("Got the reply:\n$queryResponse")
            if (queryResponse.hasErrorResponse()) {
                System.exit(1)
            }
        }
        while (true) {
            Thread.sleep(1000)
            api.transactionSync(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .createAccount(
                        String.getRandomString(10),
                        GenesisBlockBuilder.defaultDomainName,
                        GenesisBlockBuilder.defaultKeyPair.public
                    )
                    .sign(GenesisBlockBuilder.defaultKeyPair)
                    .build()
            )
        }
    }

    @Test
    fun testBigDetails() {
        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .setAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        "key",
                        String.getRandomString(4194304)
                    )
                    .sign(GenesisBlockBuilder.defaultKeyPair)
                    .build()
            ).blockingLast()
        )

        println(
            iroha.api.query(
                Query.builder(GenesisBlockBuilder.defaultAccountId, 1)
                    .disableValidation()
                    .getAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        GenesisBlockBuilder.defaultAccountId,
                        "key"
                    )
                    .buildSigned(GenesisBlockBuilder.defaultKeyPair)
            )
        )
    }

    /** Returns random string of [len] characters */
    private fun String.Companion.getRandomString(len: Int): String {
        val random = Random()
        val res = StringBuilder()
        for (i in 1..len) {
            res.append(CHAR[random.nextInt(CHAR.length)])
        }
        return res.toString()
    }

    companion object : KLogging() {
        private const val CHAR = "abcdefghijklmnopqrstuvwxyz"
    }
}
