package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
	@Value("${local.server.port}")
	private int port;

	@Test
	void donorRegistrationSearchAndDonationCooldownWorkThroughHttpApi() throws Exception {
	    HttpClient client = HttpClient.newHttpClient();
	    HttpResponse<String> dashboard = request(client, "GET", "/", null);
	    assertEquals(200, dashboard.statusCode());
	    assertTrue(dashboard.body().contains("BloodLink | Donor dashboard"));

	    String donorRequest = """
	            {
	              "name": "Priya Kumar",
	              "phone": "+91 98765 43210",
	              "email": "priya@example.com",
	              "bloodGroup": "O+",
	              "city": "Chennai",
	              "lastDonationDate": null
	            }
	            """;
	    HttpResponse<String> created = request(client, "POST", "/api/donors", donorRequest);
	    assertEquals(201, created.statusCode());
	            assertTrue(created.body().contains("\"bloodGroupLabel\":\"O+\""));
	            assertTrue(created.body().contains("\"available\":true"));

	            HttpResponse<String> invalidDonor = request(client, "POST", "/api/donors", "{}");
	            assertEquals(400, invalidDonor.statusCode());
	            assertTrue(invalidDonor.body().contains("\"fieldErrors\""));

	    HttpResponse<String> initialSearch = request(
	            client, "GET", "/api/donors/search?bloodGroup=O%2B&city=Chennai", null);
	    assertTrue(initialSearch.body().contains("\"name\":\"Priya Kumar\""));

	    String donationRequest = """
	            {"donorId": %d, "donationDate": "%s"}
	            """.formatted(idFrom(created.body()), LocalDate.now());
	    HttpResponse<String> donation = request(client, "POST", "/api/donations", donationRequest);
	    assertEquals(201, donation.statusCode());

	    HttpResponse<String> afterDonationSearch = request(
	            client, "GET", "/api/donors/search?bloodGroup=O%2B&city=Chennai", null);
	    assertEquals("[]", afterDonationSearch.body());

	    HttpResponse<String> donorAfterDonation = request(
	            client, "GET", "/api/donors/" + idFrom(created.body()), null);
	    assertTrue(donorAfterDonation.body().contains("\"available\":false"));

	    HttpResponse<String> repeatedDonation = request(client, "POST", "/api/donations", donationRequest);
	    assertEquals(409, repeatedDonation.statusCode());
	    assertTrue(repeatedDonation.body().contains("90-day cooldown"));

	    HttpResponse<String> stats = request(client, "GET", "/api/dashboard/stats", null);
	    assertTrue(stats.body().contains("\"totalDonors\":1"));
	    assertTrue(stats.body().contains("\"availableDonors\":0"));
	    HttpResponse<String> bloodGroups = request(client, "GET", "/api/dashboard/blood-groups", null);
	    assertTrue(bloodGroups.body().contains("\"label\":\"O+\""));
	    assertTrue(bloodGroups.body().contains("\"total\":1"));
	}

	private long idFrom(String json) {
	    Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
	    if (!matcher.find()) {
	        throw new AssertionError("Created donor response did not contain an id.");
	    }
	    return Long.parseLong(matcher.group(1));
	}

	private HttpResponse<String> request(HttpClient client, String method, String path, String body)
	        throws Exception {
	    HttpRequest.Builder builder = HttpRequest.newBuilder()
	            .uri(URI.create("http://localhost:" + port + path));
	    if (body == null) {
	        builder.GET();
	    } else {
	        builder.header("Content-Type", "application/json")
	                .method(method, HttpRequest.BodyPublishers.ofString(body));
	    }
	    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}
}
