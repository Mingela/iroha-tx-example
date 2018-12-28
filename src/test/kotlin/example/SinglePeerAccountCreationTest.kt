package example

import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import org.junit.jupiter.api.AfterAll
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
    fun TestWithIroha() {
        val api = iroha.api
    }
}
