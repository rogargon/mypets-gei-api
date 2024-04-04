package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Admin;
import cat.udl.eps.softarch.demo.domain.Location;
import cat.udl.eps.softarch.demo.domain.Shelter;
import cat.udl.eps.softarch.demo.domain.ShelterCertificate;
import cat.udl.eps.softarch.demo.repository.AdminRepository;
import cat.udl.eps.softarch.demo.repository.LocationRepository;
import cat.udl.eps.softarch.demo.repository.ShelterCertificateRepository;
import cat.udl.eps.softarch.demo.repository.ShelterRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

public class ValidateShelterCertificateStepDefs {

    @Autowired
    private ShelterCertificateRepository shelterCertificateRepository;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private StepDefs stepDefs;

    @Given("^A shelter with name \"([^\"]*)\"$")
    public void thereIsAShelterCreatedWithName(String name){
        Location location = new Location();
        locationRepository.save(location);
        if(shelterRepository.findByName(name) != null){
             Shelter shelter = new Shelter();
                    shelter.setName(name);
                    shelter.setEmail("test@test.com");
                    shelter.setActive(true);
                    shelter.setLocatedAt(location);
                    shelter.setMobile("000000000");
            shelterRepository.save(shelter);
        }
    }

    @And("^An admin with username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void anAdminWithUsername(String username, String password){
        Admin admin = new Admin();
        admin.setId(username);
        admin.setEmail("admin@admin.com");
        admin.setPassword(password);
        admin.encodePassword();
        adminRepository.save(admin);
    }

    @Given("^a shelter certificate associated with a shelter with name \"([^\"]*)\" with valid information created by a shelter volunteer$")
    public void aShelterCertificateWithValidInformationSentByAVolunteer(String name){
        Shelter shelter = shelterRepository.findByName(name).get(0);
        ShelterCertificate shelterCertificate = new ShelterCertificate();
        shelterCertificate.setShelterServed(shelter);
        shelterCertificate.setValidated(false);
        shelterCertificate.setExpirationDate(ZonedDateTime.now().plusMonths(6));
        shelterCertificateRepository.save(shelterCertificate);
    }

    @When("^the admin with username \"([^\"]*)\" and password \"([^\"]*)\" verifies the certificate validity associated with a shelter with name \"([^\"]*)\"$")
    public void thenAdminShouldVerifyTheCertificateValidityAssociatedWithAShelterWithName(String username, String password, String name) throws Exception {
        Shelter shelter = shelterRepository.findByName(name).get(0);
        AuthenticationStepDefs.currentUsername = username;
        AuthenticationStepDefs.currentPassword = password;

        stepDefs.result = stepDefs.mockMvc.perform(
                get( "/shelterCertificates/search/findByShelterServed?shelterServed={id}", "/shelters/"+shelter.getId())
                        .with(AuthenticationStepDefs.authenticate())
                        .accept("application/json"));

        JSONObject jsonObject = new JSONObject(stepDefs.result.andReturn().getResponse().getContentAsString());

        String shelterCertificateUri = jsonObject.getString("uri");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
        LocalDate shelterCertificateExpirationDate = LocalDate.parse(jsonObject.getString("expirationDate"), formatter);

        boolean isShelterCertificateValid = shelterCertificateExpirationDate.isAfter(LocalDate.now());
        if (!isShelterCertificateValid) {
            System.out.println("The Shelter Certificate IS not valid!" + stepDefs.result);
            Assert.fail("The Shelter Certificate IS not valid!");
        }

        stepDefs.result = stepDefs.mockMvc.perform(
                        patch( shelterCertificateUri)
                                .with(AuthenticationStepDefs.authenticate())
                                .content("{\"validated\":true}")
                                .accept("application/json"));

        jsonObject = new JSONObject(stepDefs.result.andReturn().getResponse().getContentAsString());
        boolean isShelterCertificateValidated = jsonObject.getBoolean("validated");
        if (!isShelterCertificateValidated) {
            System.out.println("The Shelter Certificate IS not validated!" + stepDefs.result);
            Assert.fail("The Shelter Certificate IS not validated!");
        }
    }

    @Given("^a shelter certificate associated with a shelter with name \"([^\"]*)\" with invalid information created by a shelter volunteer$")
    public void aShelterCertificateWithInvalidInformationSentByAVolunteer(String name){
        Shelter shelter = shelterRepository.findByName(name).get(0);
        ShelterCertificate shelterCertificate = new ShelterCertificate();
        shelterCertificate.setShelterServed(shelter);
        shelterCertificate.setValidated(false);
        shelterCertificate.setExpirationDate(ZonedDateTime.now().minusMonths(6));
        shelterCertificateRepository.save(shelterCertificate);
    }

    @When("^the admin with username \"([^\"]*)\" and password \"([^\"]*)\" rejects the certificate validity associated with a shelter with name \"([^\"]*)\"$")
    public void thenAdminShouldVerifyTheCertificateValidityAssociatedWithAShelterWithName1(String username, String password, String name) throws Exception {
        Shelter shelter = shelterRepository.findByName(name).get(0);
        AuthenticationStepDefs.currentUsername = username;
        AuthenticationStepDefs.currentPassword = password;

        stepDefs.result = stepDefs.mockMvc.perform(
                        get( "/shelterCertificates/search/findByShelterServed?shelterServed={id}", "/shelters/"+shelter.getId())
                                .with(AuthenticationStepDefs.authenticate())
                                .accept("application/json"));

        JSONObject jsonObject = new JSONObject(stepDefs.result.andReturn().getResponse().getContentAsString());

        boolean isValidated = jsonObject.getBoolean("validated");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
        LocalDate shelterCertificateExpirationDate = LocalDate.parse(jsonObject.getString("expirationDate"), formatter);

        boolean isShelterCertificateValid = shelterCertificateExpirationDate.isAfter(LocalDate.now());
        Assert.assertFalse(isShelterCertificateValid && isValidated);
    }

}
