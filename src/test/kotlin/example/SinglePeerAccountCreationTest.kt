package example

import com.google.gson.Gson
import iroha.protocol.Endpoint.TxStatus
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.BlocksQueryBuilder
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder.defaultKeyPair
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder.defaultRoleName
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

    @Test
    fun testBatchSignatories() {
        val api = iroha.api
        val tx1 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
            .setAccountDetail(GenesisBlockBuilder.defaultAccountId, "key1", "v1")
            .build()
        val tx2 = Transaction.builder(GenesisBlockBuilder.defaultAccountId)
            .setAccountDetail(GenesisBlockBuilder.defaultAccountId, "key1", "v2")
            .build()

        val batch = Utils.createTxUnsignedAtomicBatch(mutableListOf(tx1, tx2))
        val iterator = batch.iterator()
        val txx1 = iterator.next()
        val txx2 = iterator.next()
        val trueBatch = listOf<TransactionOuterClass.Transaction>(
            txx1.sign(GenesisBlockBuilder.defaultKeyPair).build(),
            txx2.build()
        )
        api.transactionListSync(trueBatch)
        Thread.sleep(5000)
        val trueBatch2 = listOf<TransactionOuterClass.Transaction>(
            trueBatch[0],
            txx2.sign(GenesisBlockBuilder.defaultKeyPair).build()
        )
        api.transactionListSync(trueBatch2)
        Thread.sleep(10000)
        trueBatch.forEach {
            val hash = Utils.hash(it)
            val txStatusSync = api.txStatusSync(hash)
            println(txStatusSync)
            assertNotEquals(TxStatus.STATEFUL_VALIDATION_FAILED, txStatusSync.txStatus)
        }
    }

    @Test
    fun testImmediateDetails() {
        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .setAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        "key",
                        String.getRandomString(10)
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

    @Test
    fun testDetailsOfOther() {
        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .createAccount("other", GenesisBlockBuilder.defaultDomainName, "".toByteArray())
                    .setAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        "key",
                        String.getRandomString(10)
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

    @Test
    fun testCAS() {
        repeat(50) {
            println("////////////////////////////////////////////////////////////////////////////////////////////////////")
            val key = String.getRandomString(10)
            Thread(
                Runnable {
                    println(
                        "1" +
                                iroha.api.transaction(
                                    Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                                        .compareAndSetAccountDetail(
                                            GenesisBlockBuilder.defaultAccountId,
                                            key,
                                            "123",
                                            null
                                        )
                                        .sign(GenesisBlockBuilder.defaultKeyPair)
                                        .build()
                                ).blockingLast()
                    )
                    println(
                        "1" +
                                iroha.api.query(
                                    Query.builder(GenesisBlockBuilder.defaultAccountId, (it * 2 + 1).toLong())
                                        .getAccountDetail(
                                            GenesisBlockBuilder.defaultAccountId,
                                            GenesisBlockBuilder.defaultAccountId,
                                            key
                                        ).buildSigned(GenesisBlockBuilder.defaultKeyPair)
                                )
                    )
                }
            ).start()
            Thread(
                Runnable {
                    println(
                        "2" +
                                iroha.api.transaction(
                                    Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                                        .compareAndSetAccountDetail(
                                            GenesisBlockBuilder.defaultAccountId,
                                            key,
                                            "456",
                                            null
                                        )
                                        .sign(GenesisBlockBuilder.defaultKeyPair)
                                        .build()
                                ).blockingLast()
                    )
                    println(
                        "2" +
                                iroha.api.query(
                                    Query.builder(GenesisBlockBuilder.defaultAccountId, (it * 2 + 2).toLong())
                                        .getAccountDetail(
                                            GenesisBlockBuilder.defaultAccountId,
                                            GenesisBlockBuilder.defaultAccountId,
                                            key
                                        ).buildSigned(GenesisBlockBuilder.defaultKeyPair)
                                )
                    )
                }
            ).start()
            Thread.sleep(2500)
        }
    }

    @Test
    fun testListJSONDetails() {
        val gson = Gson()
        val hex1 = Utils.toHex(Ed25519Sha3().generateKeypair().public.encoded)
        val hex2 = Utils.toHex(Ed25519Sha3().generateKeypair().public.encoded)
        val str = Utils.irohaEscape(gson.toJson(listOf(hex1, hex2)))
        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .setAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        "key",
                        str
                    )
                    .sign(GenesisBlockBuilder.defaultKeyPair)
                    .build()
            ).blockingLast()
        )

        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .setAccountDetail(
                        GenesisBlockBuilder.defaultAccountId,
                        "key",
                        str
                    )
                    .sign(GenesisBlockBuilder.defaultKeyPair)
                    .build()
            ).blockingLast()
        )
    }

    @Test
    fun testQuery() {
        val keyPair = Ed25519Sha3().generateKeypair()
        val domainName = "ImpiloLogTest"
        val accountName = "test2"
        val accountId = "$accountName@$domainName"
        val assetName = "login_attempt"
        val assetId = "$assetName#$domainName"
        println(
            iroha.api.transaction(
                Transaction.builder(GenesisBlockBuilder.defaultAccountId)
                    .createDomain(domainName, defaultRoleName)
                    .createAccount(accountName, domainName, keyPair.public)
                    .createAsset(assetName, domainName, 0)
                    .sign(defaultKeyPair).build()
            ).blockingLast()
        )

        println(
            iroha.api.query(
                Query.builder(accountId, 1)
                    .getAccountAssetTransactions(accountId, assetId, 5, null)
                    .buildSigned(keyPair)
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
