package example

import iroha.protocol.Primitive
import iroha.protocol.Queries
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.QueryBuilder
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder

object AccountCreatorHelper {

    private val timeOffset = 1000L
    val txList = mutableListOf<TransactionOuterClass.Transaction>()

    fun getRolesTransactions(mortalAccount: String): List<TransactionOuterClass.Transaction> {
        var time = System.currentTimeMillis() - timeOffset * 20
        val fullAccountName = "$mortalAccount@soramitsu"
        val newKeypair = Ed25519Sha3().generateKeypair()

        // Create empty role for domain
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createRole("none", emptyList())
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create domain
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createDomain("soramitsu", "none")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create account
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createAccount(fullAccountName, newKeypair.public)
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create first role
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createRole("god_of_details", listOf(Primitive.RolePermission.can_set_detail))
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create second role
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createRole("god_of_roles", listOf(Primitive.RolePermission.can_append_role))
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Append second role
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .appendRole(fullAccountName, "god_of_roles")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Append first role by self
        txList.add(TransactionBuilder(fullAccountName, time)
                .setQuorum(1)
                .appendRole(fullAccountName, "god_of_details")
                .sign(newKeypair)
                .build())
        time += timeOffset
        // Check first role
        txList.add(TransactionBuilder(fullAccountName, time)
                .setQuorum(1)
                .setAccountDetail(fullAccountName, "key", "value")
                .setAccountDetail(fullAccountName, "key1", "value2")
                .setAccountDetail(fullAccountName, "key2", "value3")
                .sign(newKeypair)
                .build())

        return txList
    }

    fun getBatchTransactions(mortalAccount: String): List<TransactionOuterClass.Transaction> {
        var time = System.currentTimeMillis() - timeOffset * 20
        val fullAccountName = "$mortalAccount@soramitsu"
        val newKeypair = Ed25519Sha3().generateKeypair()

        // Create empty role for domain
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createRole("none", emptyList())
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create domain
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createDomain("soramitsu", "none")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Create account
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createAccount(fullAccountName, newKeypair.public)
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Write something
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .setAccountDetail(fullAccountName, "key", "value")
                .setAccountDetail(fullAccountName, "key1", "value2")
                .setAccountDetail(fullAccountName, "key2", "value3")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())

        return txList
    }

    fun getQuery(accountName: String): Queries.Query {
        // Query account roles
        return QueryBuilder(GenesisBlockBuilder.defaultAccountId, System.currentTimeMillis(), 1)
                .getAccount("$accountName@soramitsu")
                .buildSigned(GenesisBlockBuilder.defaultKeyPair)
    }
}
