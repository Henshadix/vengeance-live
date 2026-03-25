package com.vengeance;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class VengeanceApiClient
{
	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(TIMEOUT)
		.build();

	@Inject
	private VengeanceConfig config;

	@Inject
	private ScheduledExecutorService executor;

	public void postDropAsync(String playerName, int itemId, String itemName,
		int amount, Long gpValueTotal, String source)
	{
		if (!config.enabled())
		{
			return;
		}
		String base = config.apiBaseUrl().trim();
		if (base.isEmpty())
		{
			return;
		}
		while (base.endsWith("/"))
		{
			base = base.substring(0, base.length() - 1);
		}
		String url = base + "/api/drops";

		JsonObject body = new JsonObject();
		body.addProperty("playerName", playerName);
		body.addProperty("itemId", itemId);
		body.addProperty("amount", amount);
		body.addProperty("timestamp", System.currentTimeMillis());
		body.addProperty("source", source);
		if (itemName != null && !itemName.isEmpty())
		{
			body.addProperty("itemName", itemName);
		}
		if (gpValueTotal != null)
		{
			body.addProperty("gpValueTotal", gpValueTotal);
		}

		final String json = body.toString();
		executor.execute(() -> send(url, json));
	}

	private void send(String url, String json)
	{
		try
		{
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(TIMEOUT)
				.header("Content-Type", "application/json; charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300)
			{
				log.warn("Vengeance API POST {} -> {}: {}", url, response.statusCode(), response.body());
			}
		}
		catch (Exception e)
		{
			log.warn("Vengeance API POST failed: {}", e.getMessage());
		}
	}
}
