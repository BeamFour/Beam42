package org.redukti.importers.obench;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Fetches a prescription straight from the PhotonsToPhotos Optical Bench rather
 * than from a local file.
 *
 * <p>Every lens on the Optical Bench Hub is published at a predictable address
 * built from the patent number and the example number, so a patent and example
 * are all the caller has to supply.
 *
 * @see <a href="https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm">Optical Bench Hub</a>
 */
public final class ObenchFetcher {

    public static final String DATA_BASE_URL =
            "https://www.photonstophotos.net/GeneralTopics/Lenses/OpticalBench/Data/";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private ObenchFetcher() {}

    /** Raised when the patent and example name no lens the Optical Bench holds. */
    public static class NotFoundException extends IOException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /**
     * The file name the Optical Bench publishes a lens under, for example
     * JP1993-034592_Example02. A purely numeric example is padded to two digits
     * to match the site's own naming; anything else is passed through, so
     * suffixed examples such as 08P still work.
     */
    public static String fileNameFor(String patent, String example) {
        if (patent == null || patent.isBlank())
            throw new IllegalArgumentException("a patent number is required, e.g. JP1993-034592");
        if (example == null || example.isBlank())
            throw new IllegalArgumentException("an example is required, e.g. 2 or 08P");
        String trimmed = example.trim();
        String normalized = trimmed.chars().allMatch(Character::isDigit) && trimmed.length() < 2
                ? "0" + trimmed
                : trimmed;
        return patent.trim() + "_Example" + normalized + ".txt";
    }

    public static String urlFor(String patent, String example) {
        return DATA_BASE_URL + fileNameFor(patent, example);
    }

    /**
     * Downloads the prescription for a patent and example.
     *
     * @throws NotFoundException if the Optical Bench has no such patent or example
     * @throws IOException if the download fails for any other reason
     */
    public static String fetch(String patent, String example) throws IOException, InterruptedException {
        return fetchUrl(urlFor(patent, example), patent + " example " + example);
    }

    /** Downloads a prescription from an explicit Optical Bench URL. */
    public static String fetchUrl(String url, String what) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            throw new IOException("Could not reach the Optical Bench at " + url + ": " + e.getMessage(), e);
        }
        if (response.statusCode() == 404)
            throw new NotFoundException("Patent / example not found on the Optical Bench: "
                    + what + " (looked for " + url + ")");
        if (response.statusCode() != 200)
            throw new IOException("Optical Bench returned HTTP " + response.statusCode()
                    + " for " + url);
        String body = response.body();
        // A miss can also come back as an error page rather than a 404, so check
        // that what arrived actually looks like a prescription before using it.
        if (body == null || !body.contains("[lens data]"))
            throw new NotFoundException("The Optical Bench response for " + what
                    + " is not a prescription (no [lens data] section): " + url);
        return body;
    }
}
