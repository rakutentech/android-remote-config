package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
open class SignatureVerifierSpec : RobolectricBaseSpec() {

    internal val mockCache: PublicKeyCache = mock()

    /*
        The following commands were used to generate the public key and signature used in these tests

        ## Generate a private key
        $ openssl ec -ciphername secp256r1 -out params.pem
        $ openssl ecparam -in params.pem -genkey -out privkey.pem

        ## Extract the public key
        $ openssl ec -in privkey.pem -pubout -out pubkey.pem
        $ cat pubkey.pem | openssl ec -pubin -text -noout
            read EC key
            Private-Key: (256 bit)
            pub:
                04:8d:b3:66:be:7a:82:19:cc:2d:70:4c:78:2e:1b:
                90:85:60:eb:3a:45:0f:62:02:21:2e:d5:e9:c5:a8:
                f2:9f:0b:92:26:ee:7a:bb:21:3b:e1:dd:e6:bb:ba:
                a0:11:4b:95:49:3a:20:51:4e:21:57:c7:2a:23:9e:
                9c:72:c1:34:86
            ASN1 OID: prime256v1
            NIST CURVE: P-256
        ## Use bytes from `pub:` section above and convert to base64 to get public key
        $ echo '04:8d:b3:66:be:7a:82:19:cc:2d:70:4c:78:2e:1b:90:85:60:eb:3a:45:0f:62:02:21:2e:d5:\
        e9:c5:a8:f2:9f:0b:92:26:ee:7a:bb:21:3b:e1:dd:e6:bb:ba:a0:11:4b:95:49:3a:20:51:4e:21:57:c7:\
        2a:23:9e:9c:72:c1:34:86' | tr -d : | xxd -r -ps | base64
            BI2zZr56ghnMLXBMeC4bkIVg6zpFD2ICIS7V6cWo8p8LkibuershO+Hd5ru6oBFLlUk6IFFOIVfHKiOenHLBNIY=

        ## Generate the signature
        $ openssl dgst -sha256 -sign privkey.pem -out signature.txt payload.txt
        $ openssl dgst -sha256 -verify pubkey.pem -signature signature.bin payload.txt
            Verified OK
        $ cat signature.bin | base64
            MEUCIHRXIgQhyASpyCP1Lg0ZSn2/bUbTq6U7jpKBa9Ow/1OTAiEA4jAq48uDgNl7UM7LmxhiRhPPNnTolokScTq5ijbp5fU
     */
    val publicKey = "BI2zZr56ghnMLXBMeC4bkIVg6zpFD2ICIS7V6cWo8p8LkibuershO+Hd5ru6oBFLlUk6IFFOIVfHKiOenHLBNIY="
    val body = """{"testKey": "test_value"}"""
    val signature = "MEUCIHRXIgQhyASpyCP1Lg0ZSn2/bUbTq6U7jpKBa9Ow/1OTAiEA4jAq48uDgNl7UM7LmxhiRhPPNnTolokScTq5ijbp5fU="

    @Before
    fun setup() {
        When calling mockCache["test_key_id"] itReturns publicKey
    }
}

class CachedPublicKeySignatureVerifierSpec : SignatureVerifierSpec() {
    @Test
    fun `should verify the signature`() {
        val verifier = SignatureVerifier(mockCache)

        verifier.verifyCached(
            "test_key_id",
            body.byteInputStream(),
            signature
        ) shouldEqual true
    }

    @Test
    fun `should not verify the signature when message has been modified`() {
        val verifier = SignatureVerifier(mockCache)

        verifier.verifyCached(
            "test_key_id",
            "wrong message".byteInputStream(),
            signature
        ) shouldEqual false
    }

    @Test
    fun `should not verify the signature when public key is not cached`() {
        val verifier = SignatureVerifier(mockCache)

        verifier.verifyCached(
            "random_key_id",
            "wrong message".byteInputStream(),
            signature
        ) shouldEqual false
    }
}

class FetchedPublicKeySignatureVerifierSpec : SignatureVerifierSpec() {
    @Test
    fun `should verify the signature`() {
        val verifier = SignatureVerifier(mockCache)

        verifier.verifyFetched(
            "test_key_id",
            body.byteInputStream(),
            signature
        ) shouldEqual true
    }

    @Test
    fun `should not verify the signature when message has been modified`() {
        val verifier = SignatureVerifier(mockCache)

        verifier.verifyFetched(
            "test_key_id",
            "wrong message".byteInputStream(),
            signature
        ) shouldEqual false
    }

    @Test
    fun `should fetch the public key when it is not cached`() {
        When calling mockCache["test_key_id"] itReturns null
        When calling mockCache.fetch("test_key_id") itReturns publicKey

        val verifier = SignatureVerifier(mockCache)

        verifier.verifyFetched(
            "test_key_id",
            body.byteInputStream(),
            signature
        ) shouldEqual true
    }
}
