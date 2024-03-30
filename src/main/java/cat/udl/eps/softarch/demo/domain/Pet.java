package cat.udl.eps.softarch.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class Pet extends UriEntity<Long> {
    @Id
    Long id;

    String name;
    boolean isAdopted;
    String color;
    String size;
    Double weight;
    String age;
    String description;
    String breed;


    @ManyToOne
    public Shelter isIn;
}
