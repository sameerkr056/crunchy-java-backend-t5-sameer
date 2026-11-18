package com.crunchycode.cabbooking.support;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Small helpers shared by the controller tests. */
public final class RequestHelper {

    public static final String DRIVER_JSON =
            "{\"name\":\"Test Driver\",\"email\":\"test.driver@example.com\",\"phone\":\"9876543210\",\"licenseNumber\":\"JH0120239999\"}";
    public static final String RIDE_JSON =
            "{\"passengerName\":\"Test Passenger\",\"pickupLocation\":\"Station\",\"dropLocation\":\"Airport\",\"fare\":100.00,\"driverId\":1}";
    public static final String STATUS_JSON = "{\"status\":\"COMPLETED\"}";
    public static final String VEHICLE_TYPE_JSON = "{\"name\":\"Test Van\",\"description\":\"A van for tests\"}";

    private RequestHelper() {
    }

    public static MockHttpServletRequestBuilder build(String method, String url, String body) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), url);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder;
    }
}
