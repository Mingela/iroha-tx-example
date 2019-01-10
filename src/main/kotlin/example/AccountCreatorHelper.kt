package example

import iroha.protocol.Primitive
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder

object AccountCreatorHelper {

    private val timeOffset = 1000L
    val txList = mutableListOf<TransactionOuterClass.Transaction>()

    fun getTransactions(mortalAccount: String): List<TransactionOuterClass.Transaction> {
        var time = System.currentTimeMillis() - timeOffset * 20

        val newKeypair = Ed25519Sha3().generateKeypair()

        // Create account
        txList.add(TransactionBuilder(GenesisBlockBuilder.defaultAccountId, time)
                .setQuorum(1)
                .createAccount(mortalAccount, newKeypair.public)
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
                .appendRole(mortalAccount, "god_of_roles")
                .sign(GenesisBlockBuilder.defaultKeyPair)
                .build())
        time += timeOffset
        // Append first role by self
        txList.add(TransactionBuilder(mortalAccount, time)
                .setQuorum(1)
                .appendRole(mortalAccount, "god_of_details")
                .sign(newKeypair)
                .build())
        time += timeOffset
        // Check first role
        txList.add(TransactionBuilder(mortalAccount, time)
                .setQuorum(1)
                .setAccountDetail(mortalAccount, "key", "value")
                .setAccountDetail(mortalAccount, "key1", "value2")
                .setAccountDetail(mortalAccount, "key2", "value3")
                .sign(newKeypair)
                .build())

        return txList
    }
}
