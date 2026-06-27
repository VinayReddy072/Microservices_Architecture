package com.labplatform.common.event;
public class BookingCreatedEventV1 extends BookingCreatedEvent {
    public BookingCreatedEventV1() {
        super();
    }
    @Override
    public String getEventVersion() {
        return "1.0";
    }
}
