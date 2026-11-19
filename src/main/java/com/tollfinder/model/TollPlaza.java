package com.tollfinder.model;

import jakarta.persistence.*;

/** The unique key is the location: names repeat and can be missing in the source data. */
@Entity
@Table(name = "toll_plaza", uniqueConstraints = @UniqueConstraint(columnNames = {"latitude", "longitude"}))
public class TollPlaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    protected TollPlaza() {
    }

    public TollPlaza(String name, String state, double latitude, double longitude) {
        this.name = name;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinate location() {
        return new Coordinate(latitude, longitude);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getState() { return state; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public void setName(String name) { this.name = name; }
    public void setState(String state) { this.state = state; }
}
