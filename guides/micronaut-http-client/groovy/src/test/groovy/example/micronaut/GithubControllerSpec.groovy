package example.micronaut

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.StreamingHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import spock.lang.Shared
import spock.lang.Specification
import java.util.regex.Pattern
import java.util.stream.Stream

@MicronautTest // <1>
class GithubControllerSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient client // <2>

    @Inject
    @Client("/")
    StreamingHttpClient streamingClient

    @Shared
    Pattern MICRONAUT_RELEASE = Pattern.compile("([Micronaut|Micronaut Framework] [0-9].[0-9].[0-9]([0-9])?( (RC|M)[0-9])?|v3.6.5)");


    void 'verify github releases can be fetched with low level HttpClient'() {
        when:
        HttpRequest request = HttpRequest.GET('/github/releases-lowlevel')

        HttpResponse<List<GithubRelease>> rsp = client.toBlocking().exchange(request, // <3>
                Argument.listOf(GithubRelease)) // <4>

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK // <5>
        rsp.body() // <6>

        when:
        List<GithubRelease> releases = rsp.body()

        then:
        releases
        releases.stream()
                .map(GithubRelease::getName)
                .allMatch(name -> MICRONAUT_RELEASE.matcher(name)
                        .find())
    }

    void 'verify github releases can be fetched with compile-time autogenerated @Client'() {
        when:
        HttpRequest request = HttpRequest.GET('/github/releases')

        Stream<GithubRelease> githubReleases = Flux.from(streamingClient.jsonStream(request, GithubRelease)).toStream() // <7>

        then:
        githubReleases
                .map(GithubRelease::getName)
                .allMatch(name -> MICRONAUT_RELEASE.matcher(name)
                        .find())
    }
}
