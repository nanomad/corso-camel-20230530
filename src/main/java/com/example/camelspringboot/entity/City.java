package com.example.camelspringboot.entity;

import javax.persistence.*;

@Entity
@Table(name = "CITIES")
@NamedQueries(
        @NamedQuery(name = "findTokio", query = "SELECT c FROM City c where c.city='Tokio'")
)
public class City {
    @Id
    @Column(name = "city")
    private String city;

    @Column(name = "iso2")
    private String iso2;

    @Column(name = "iso3")
    private String iso3;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIso2() {
        return iso2;
    }

    public void setIso2(String iso2) {
        this.iso2 = iso2;
    }

    public String getIso3() {
        return iso3;
    }

    public void setIso3(String iso3) {
        this.iso3 = iso3;
    }

    @Override
    public String toString() {
        return "City{" +
                "city='" + city + '\'' +
                ", iso2='" + iso2 + '\'' +
                ", iso3='" + iso3 + '\'' +
                '}';
    }
}
