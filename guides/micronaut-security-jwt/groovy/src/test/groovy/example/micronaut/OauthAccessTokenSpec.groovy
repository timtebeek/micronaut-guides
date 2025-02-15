package example.micronaut

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.endpoints.TokenRefreshRequest
import io.micronaut.security.token.render.AccessRefreshToken
import io.micronaut.security.token.render.BearerAccessRefreshToken
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest(rollback = false) // <1>
class OauthAccessTokenSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient client // <2>

    @Shared
    @Inject
    RefreshTokenRepository refreshTokenRepository

    void "Verify JWT access token refresh works"() {
        given:
        String username = 'sherlock'

        when: 'login endpoint is called with valid credentials'
        def creds = new UsernamePasswordCredentials(username, "password")
        HttpRequest request = HttpRequest.POST('/login', creds)
        BearerAccessRefreshToken rsp = client.toBlocking().retrieve(request, BearerAccessRefreshToken)

        then: 'the refresh token is saved to the database'
        new PollingConditions().eventually {
            assert refreshTokenRepository.count() == old(refreshTokenRepository.count()) + 1
        }

        and: 'response contains an access token token'
        rsp.accessToken

        and: 'response contains a refresh token'
        rsp.refreshToken

        when:
        sleep(1_000) // sleep for one second to give time for the issued at `iat` Claim to change
        AccessRefreshToken refreshResponse = client.toBlocking().retrieve(HttpRequest.POST('/oauth/access_token',
                new TokenRefreshRequest(TokenRefreshRequest.GRANT_TYPE_REFRESH_TOKEN, rsp.refreshToken)), AccessRefreshToken) // <1>

        then:
        refreshResponse.accessToken
        refreshResponse.accessToken != rsp.accessToken // <2>

        cleanup:
        refreshTokenRepository.deleteAll()
    }
}
