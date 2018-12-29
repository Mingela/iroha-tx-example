package example

import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import javax.xml.bind.DatatypeConverter

object AccountCreatorHelper {

    private val timeOffset = 1000L
    val txList = mutableListOf<TransactionOuterClass.Transaction>()

    fun getAccountTransactionList(mortalAccount: String): List<TransactionOuterClass.Transaction> {
        var time = System.currentTimeMillis() - timeOffset * 20

        val newKeypair = Ed25519Sha3().generateKeypair()

        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createAccount(mortalAccount, newKeypair.public)
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset * 5
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .setAccountDetail(mortalAccount, "key", "value")
                .setAccountDetail(mortalAccount, "key1", "value2")
                .setAccountDetail(mortalAccount, "key2", "value3")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())

        return txList
    }

    fun getTransactionsHashes(): List<String> {
        return txList.map { DatatypeConverter.printHexBinary(Utils.hash(it)) }
    }
}
