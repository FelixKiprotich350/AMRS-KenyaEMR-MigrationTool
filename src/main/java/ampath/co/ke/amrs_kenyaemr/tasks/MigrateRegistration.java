package ampath.co.ke.amrs_kenyaemr.tasks;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSSamples;
import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.*;
import ampath.co.ke.amrs_kenyaemr.service.*;
import ampath.co.ke.amrs_kenyaemr.tasks.payloads.RegisterOpenMRSPayload;
import jakarta.persistence.Column;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import static org.thymeleaf.util.StringUtils.substring;

public class MigrateRegistration {

    public static String samplePatientList = AMRSSamples.getPersonIdList();

    @Autowired
    AMRSPatientServices amrsPatientServices;

    public static void users(String server, String username, String password, String locations, String parentUUID, AMRSUserServices amrsUserServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
        String sql = "";
        List<AMRSUsers> amrsVisitsList = amrsUserServices.findFirstByOrderByIdDesc();
        if (amrsVisitsList.size() > 0) {
            String visitId = amrsVisitsList.get(0).getUser_id();
            sql = "select \n" +
                    "                u.uuid,\n" +
                    "                u.user_id,\n" +
                    "                u.system_id,\n" +
                    "                u.username,\n" +
                    "                pn.given_name,\n" +
                    "                pn.family_name,\n" +
                    "                pn.middle_name,\n" +
                    "                p.gender,\n" +
                    "                p.birthdate,\n" +
                    "                pa.address1,\n" +
                    "                pa.county_district,\n" +
                    "                pa.address4,\n" +
                    "                pa.address5,\n" +
                    "                pa.address6,\n" +
                    "                p.dead,\n" +
                    "                p.birthdate_estimated \n" +
                    "                from amrs.encounter e \n" +
                    "                inner join amrs.users u on e.creator =u.user_id\n" +
                    "                inner join amrs.person p on p.person_id=u.person_id\n" +
                    "                inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                    "                inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                    "                where location_id in (" + locations + ") and u.user_id >" + visitId + "  \n" +
                    "                group by u.user_id\n" +
                    "                order by u.user_id asc";
        } else {
            sql = "select \n" +
                    "                u.uuid,\n" +
                    "                u.user_id,\n" +
                    "                u.system_id,\n" +
                    "                u.username,\n" +
                    "                pn.given_name,\n" +
                    "                pn.family_name,\n" +
                    "                pn.middle_name,\n" +
                    "                p.gender,\n" +
                    "                p.birthdate,\n" +
                    "                pa.address1,\n" +
                    "                pa.county_district,\n" +
                    "                pa.address4,\n" +
                    "                pa.address5,\n" +
                    "                pa.address6,\n" +
                    "                p.dead,\n" +
                    "                p.birthdate_estimated \n" +
                    "                from amrs.encounter e \n" +
                    "                inner join amrs.users u on e.creator =u.user_id\n" +
                    "                inner join amrs.person p on p.person_id=u.person_id\n" +
                    "                inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                    "                inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                    "                where location_id in (" + locations + ") \n" +
                    "                group by u.user_id\n" +
                    "                order by u.user_id asc";
        }

        System.out.println("locations " + locations + " parentUUID " + parentUUID);
        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(sql);
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {
            System.out.println("User id " + rs.getString(1));
            List<AMRSUsers> afyastatErrors = amrsUserServices.getUserByLocation(rs.getString(1), parentUUID);
            if (afyastatErrors.size() == 0) {
                AMRSUsers ae = new AMRSUsers();
                ae.setUuid(rs.getString(1));
                ae.setUser_id(rs.getString(2));
                ae.setSystem_id(rs.getString(3));
                ae.setUsername(rs.getString(4));
                ae.setGiven_name(rs.getString(5));
                ae.setFamily_name(rs.getString(6));
                ae.setMiddle_name(rs.getString(7));
                ae.setGender(rs.getString(8));
                ae.setBirthdate(rs.getString(9));
                ae.setAddress1(rs.getString(10));
                ae.setCounty_district(rs.getString(11));
                ae.setAddress4(rs.getString(12));
                ae.setAddress5(rs.getString(13));
                ae.setAddress6(rs.getString(14));
                ae.setDead(rs.getString(15));
                ae.setBirthdate_estimate(rs.getString(16));
                ae.setAmrsLocation(parentUUID);
                amrsUserServices.save(ae);

                //Migate user
                RegisterOpenMRSPayload.users(ae, amrsUserServices, url, auth);


            }
//con.close();
        }
    }

    //Patients
    public static void patients(String server, String username, String password, String locations, String parentUUID, AMRSPatientServices amrsPatientServices, AMRSIdentifiersService amrsIdentifiersService, AMRSPersonAtrributesService amrsPersonAtrributesService,AMRSPatientStatusService amrsPatientStatusService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
        List<AMRSPatients> patientsListt = amrsPatientServices.findFirstByOrderByIdDesc();

        String sql = "";
        if (patientsListt.size() == 0) {
            sql = "select  \n" +
                    "                   p.uuid, \n" +
                    "                   p.person_id, \n" +
                    "                   pn.given_name, \n" +
                    "                   pn.family_name, \n" +
                    "                   pn.middle_name, \n" +
                    "                   p.gender, \n" +
                    "                   p.birthdate, \n" +
                    "                   pa.address1, \n" +
                    "                   pa.county_district, \n" +
                    "                   pa.address4, \n" +
                    "                   pa.address5, \n" +
                    "                   pa.address6, \n" +
                    "                   p.dead, \n" +
                    "                   p.cause_of_death,\n" +
                    "                   p.death_date,\n" +
                    "case \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then 142412\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then 114100\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 58 then 112141\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 60 then 115835\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 84 then 84\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 86 then 86\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 102 then 102\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 123 then 116128\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then 112234\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then 507\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then 117399\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then 1067\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then 1107\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then 125561\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then 159\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then 137296\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then 5041\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then 119975\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then 5622\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then 139444\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then 134612\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then 145717 \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then 133814\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then 155762 \n" +
                    "    else 5622\n" +
                    "\tend as kmr_concept_id," +
                    "case\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then '142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then '114100AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 58 then '112141AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 60 then '115835AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 84 then '84AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 86 then '86AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 102 then '102AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 123 then '116128AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then '112234AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then '507AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then '117399AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then '1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then '125561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then '159AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then '137296AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then '5041AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then '119975AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then '139444AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then '134612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then '145717AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then '133814AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then '155762AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\tend as kmr_concept_uuid, \n" +
                    "                   p.birthdate_estimated, \n" +
                    "                   p.voided, \n" +
                    "                   l.location_id, \n" +
                    "                   l.name location_name, \n" +
                    "                   pa.address1 county,  \n" +
                    "                   pa.address2 sub_county, \n" +
                    "                   pa.city_village, \n" +
                    "                   pa.state_province,  \n" +
                    "                   pa.county_district, \n" +
                    "                   pa.address3 landmark \n" +
                    "                   from amrs.encounter e  \n" +
                    "                   inner join amrs.patient pt on e.patient_id =pt.patient_id and pt.voided=0 \n" +
                    "                   inner join amrs.person p on p.person_id=pt.patient_id and p.voided=0 \n" +
                    "                   inner join amrs.person_name pn on pn.person_id=p.person_id and pn.voided=0 \n" +
                    "                   inner join amrs.person_address pa on pa.person_id=p.person_id and pa.preferred=1 and pa.voided=0 \n" +
                    "                   inner join amrs.location l on e.location_id=l.location_id \n" +
                    "                   where l.uuid in ( " + locations + " ) and p.voided=0  and p.person_id in ( " + samplePatientList + ") and p.voided=0  -- and p.person_id >  + pid +    \n" +
                    "                   group by pt.patient_id \n" +
                    "                   order by e.patient_id asc ";
            System.out.println("SQL ID is " + sql);
        } else {
            String pid = patientsListt.get(0).getPersonId();
            System.out.println("Person ID is " + pid);
            System.out.println("SQL ID is " + sql);

            sql = "select  \n" +
                    "                   p.uuid, \n" +
                    "                   p.person_id, \n" +
                    "                   pn.given_name, \n" +
                    "                   pn.family_name, \n" +
                    "                   pn.middle_name, \n" +
                    "                   p.gender, \n" +
                    "                   p.birthdate, \n" +
                    "                   pa.address1, \n" +
                    "                   pa.county_district, \n" +
                    "                   pa.address4, \n" +
                    "                   pa.address5, \n" +
                    "                   pa.address6, \n" +
                    "                   p.dead, \n" +
                    "                   p.cause_of_death,\n" +
                    "                   p.death_date,\n" +
                    "case \n" +
                    "\twhen p.dead = 1 and c.concept_id = 16 then 142412\n" +
                    "\twhen p.dead = 1 and c.concept_id = 43 then 114100\n" +
                    "\twhen p.dead = 1 and c.concept_id = 58 then 112141\n" +
                    "\twhen p.dead = 1 and c.concept_id = 60 then 115835\n" +
                    "\twhen p.dead = 1 and c.concept_id = 84 then 84\n" +
                    "\twhen p.dead = 1 and c.concept_id = 86 then 86\n" +
                    "\twhen p.dead = 1 and c.concept_id = 102 then 102\n" +
                    "\twhen p.dead = 1 and c.concept_id = 123 then 116128\n" +
                    "\twhen p.dead = 1 and c.concept_id = 148 then 112234\n" +
                    "\twhen p.dead = 1 and c.concept_id = 507 then 507\n" +
                    "\twhen p.dead = 1 and c.concept_id = 903 then 117399\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1067 then 1067\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1107 then 1107\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1571 then 125561\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1593 then 159\n" +
                    "\twhen p.dead = 1 and c.concept_id = 2375 then 137296\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5041 then 5041\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5547 then 119975\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5622 then 5622\n" +
                    "\twhen p.dead = 1 and c.concept_id = 6483 then 139444\n" +
                    "\twhen p.dead = 1 and c.concept_id = 7257 then 134612\n" +
                    "\twhen p.dead = 1 and c.concept_id = 7971 then 145717 \n" +
                    "\twhen p.dead = 1 and c.concept_id = 10366 then 133814\n" +
                    "\twhen p.dead = 1 and c.concept_id = 12038 then 155762 \n" +
                    "    else 5622\n" +
                    "\tend as kmr_concept_id," +
                    "case\n" +
                    "\twhen p.dead = 1 and c.concept_id = 16 then '142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 43 then '114100AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 58 then '112141AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 60 then '115835AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 84 then '84AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 86 then '86AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 102 then '102AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  c.concept_id = 123 then '116128AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 148 then '112234AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 507 then '507AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 903 then '117399AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1067 then '1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1571 then '125561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 1593 then '159AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 2375 then '137296AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5041 then '5041AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5547 then '119975AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 6483 then '139444AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 7257 then '134612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 7971 then '145717AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and c.concept_id = 10366 then '133814AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                    "\twhen p.dead = 1 and c.concept_id = 12038 then '155762AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\tend as kmr_concept_uuid, \n" +
                    "                   p.birthdate_estimated, \n" +
                    "                   p.voided, \n" +
                    "                   l.location_id, \n" +
                    "                   l.name location_name, \n" +
                    "                   pa.address1 county,  \n" +
                    "                   pa.address2 sub_county, \n" +
                    "                   pa.city_village, \n" +
                    "                   pa.state_province,  \n" +
                    "                   pa.county_district, \n" +
                    "                   pa.address3 landmark \n" +
                    "                   from amrs.encounter e  \n" +
                    "                   inner join amrs.patient pt on e.patient_id =pt.patient_id and pt.voided=0\n" +
                    "                   inner join amrs.person p on p.person_id=pt.patient_id  and p.voided=0\n" +
                    "                   inner join amrs.person_name pn on pn.person_id=p.person_id and pn.voided=0 \n" +
                    "                   inner join amrs.person_address pa on pa.person_id=p.person_id and pa.preferred=1 and pa.voided=0 \n" +
                    "                   inner join amrs.location l on e.location_id=l.location_id \n" +
                    "where l.uuid in (" + locations + ") and p.voided=0  and  p.person_id in (" + samplePatientList + ") \n" + //and p.person_id >"+ pid +"
                    "group by pt.patient_id\n" +
                    "order by e.patient_id asc";

        }
        System.out.println("locations " + locations + " parentUUID " + parentUUID);
        //System.out.println("SQL "+ sql);
        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(sql);
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {

            System.out.println("User id " + rs.getString(1));
            //List<AMRSPatients> patientsList = amrsPatientServices.getPatientByLocation(rs.getString("person_id"), parentUUID);

            List<AMRSPatients> patientsList = amrsPatientServices.getPatientByStatus(rs.getString("person_id"));
            if (patientsList.size() == 0) {

                String person_id = rs.getString("person_id");
                AMRSPatients ae = new AMRSPatients();
                ae.setUuid(rs.getString("uuid"));
                ae.setPersonId(rs.getString("person_id"));
                ae.setGiven_name(rs.getString("given_name"));
                ae.setFamily_name(rs.getString("family_name"));
                ae.setMiddle_name(rs.getString("middle_name"));
                ae.setGender(rs.getString("gender"));
                ae.setBirthdate(rs.getString("birthdate"));
                ae.setAddress1(rs.getString("address1"));//county
                ae.setCounty_district(rs.getString("county_district"));
                ae.setAddress4(rs.getString("address4")); //
                ae.setAddress5(rs.getString("address5")); //
                ae.setAddress6(rs.getString("address6")); //
                ae.setCounty(rs.getString("county"));
                ae.setSubcounty(rs.getString("sub_county"));
                ae.setLandmark(rs.getString("landmark"));
                ae.setVillage(rs.getString("city_village"));
                ae.setDead(rs.getString("dead"));
                ae.setCauseOfDead(rs.getString("cause_of_death"));
                ae.setBirthdate_estimated(rs.getString("birthdate_estimated"));
                ae.setKenyaemrCauseOfDead(rs.getString("kmr_concept_id"));
                ae.setKenyaemrCauseOfDeadUuid(rs.getString("kmr_concept_uuid"));
                ae.setVoided(rs.getString("voided"));
                ae.setLocation_id(rs.getString("location_id"));
                ae.setCityVillage(rs.getString("city_village"));
                ae.setAddress2(rs.getString("Sub_county"));//Sub_county;
                ae.setAddress3(rs.getString("landmark"));//landmark;
                ae.setCounty_district(rs.getString("county_district"));
                ae.setStateProvince(rs.getString("state_province"));
                ae.setParentlocationuuid(parentUUID);
                amrsPatientServices.save(ae);

                String identifiersSQl = "select pi.patient_id,\n" +
                        "pit.uuid identifer_uuid," +
                        "pi.identifier,pi.preferred,pi.voided,pi.location_id\n" +
                        "from amrs.patient_identifier pi\n" +
                        "join amrs.patient_identifier_type pit on pi.identifier_type=pit.patient_identifier_type_id\n" +
                        "inner join amrs.person p on p.person_id =pi.patient_id\n" +
                        "where pi.voided=0 and pi.patient_id=" + rs.getString("person_id") + "\n" +
                        "order by pi.patient_id desc";
                //identifers
                Statement stmtID = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                ResultSet rsID = stmtID.executeQuery(identifiersSQl);
                rsID.last();
                x = rsID.getRow();
                rsID.beforeFirst();
                while (rsID.next()) {
                    List<AMRSIdentifiers> identifiers = amrsIdentifiersService.getPatientByLocation(rsID.getString("patient_id"), parentUUID);
                    if (identifiers.size() == 0) {
                        AMRSIdentifiers iden = new AMRSIdentifiers();
                        String pref = "";
                        if (rsID.getString(4).equals("0")) {
                            pref = "false";
                        } else {
                            pref = "true";
                        }
                        String identifer = "";
                        if (rsID.getString(2).equals("f2d6ff1a-8440-4d35-a150-1d4b5a930c5e")) {
                            identifer = rsID.getString(3).replace("-", "");

                        } else {
                            identifer = rsID.getString(3);
                        }
                        iden.setPatientid(rsID.getString(1));
                        iden.setUuid(rsID.getString(2));
                        iden.setIdentifier(identifer);
                        iden.setPreferred(pref);
                        iden.setVoided(rsID.getString(5));
                        iden.setLocation(rsID.getString(6));
                        iden.setKenyaemr_uuid(Mappers.identifers(rsID.getString(2)));
                        iden.setParentlocationuuid(parentUUID);

                        amrsIdentifiersService.save(iden);

                    }
                    //end of identifers
                    //Person Attributes
                    String pattreibutesSQl =
                            "select  pa.person_id,\n" +
                                    "pt.person_attribute_type_id,\n" +
                                    "pt.name,\n" +
                                    "pa.value,\n" +
                                    "case when pt.person_attribute_type_id=10 then 'b2c38640-2603-4629-aebd-3b54f33f1e3a' -- Telephone\n" +
                                    "when pt.person_attribute_type_id =12 then '830bef6d-b01f-449d-9f8d-ac0fede8dbd3'\n" +
                                    "when pt.person_attribute_type_id =25 then '342a1d39-c541-4b29-8818-930916f4c2dc' -- contact\n" +
                                    "when pt.person_attribute_type_id =0 then '7cf22bec-d90a-46ad-9f48-035952261294' -- Kin Address\n" +
                                    " when pt.person_attribute_type_id =0 then '94614350-84c8-41e0-ac29-86bc107069be' -- alternative phone\n" +
                                    "WHEN pt.person_attribute_type_id = 10 THEN 'b2c38640-2603-4629-aebd-3b54f33f1e3a'\n" + "       " +
                                    " WHEN pt.person_attribute_type_id = 12 THEN '830bef6d-b01f-449d-9f8d-ac0fede8dbd3'\n" +
                                    "        WHEN pt.person_attribute_type_id = 25 THEN '342a1d39-c541-4b29-8818-930916f4c2dc'\n" +
                                    "        WHEN pt.person_attribute_type_id = 0 THEN '7cf22bec-d90a-46ad-9f48-035952261294' \n" +
                                    "        WHEN pt.person_attribute_type_id = 40 THEN '94614350-84c8-41e0-ac29-86bc107069be'\n" +
                                    "\t\tWHEN pt.person_attribute_type_id = 163 THEN 'accb7273-ef29-11ed-8ec5-70b5e8686cf7'\n" +
                                    "        WHEN pt.person_attribute_type_id = 2 THEN '8d8718c2-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 3 THEN '8d871afc-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 5 THEN '8d871f2a-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 60 THEN 'b8d0b331-1d2d-4a9a-b741-1816f498bdb6'\n" +
                                    "        when pt.person_attribute_type_id = 7  then '8d87236c-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        when pt.person_attribute_type_id = 59  then 'd0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5'\n" +
                                    "        when pt.person_attribute_type_id = 165  then '752a0331-5293-4aa5-bf46-4d51aaf2cdc5'\n" +
                                    "        when pt.person_attribute_type_id = 164  then '869f623a-f78e-4ace-9202-0bed481822f5'\n" +
                                    "        when pt.person_attribute_type_id = 4  then '8d871d18-c2cc-11de-8d13-0010c6dffd0f' \n" +
                                    "      when pt.person_attribute_type_id = 1  then  '8d871386-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "else null end as kenyaemruuid\n" +
                                    " from amrs.person_attribute pa\n" +
                                    " inner join amrs.person_attribute_type pt on pa.person_attribute_type_id = pt.person_attribute_type_id\n" +
                                    "where pa.person_id  in (" + rs.getString("person_id") + ") and pa.voided=0";
                    Statement stmtPA = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
                    ResultSet rsPA = stmtPA.executeQuery(pattreibutesSQl);
                    rsPA.last();
                    x = rsPA.getRow();
                    rsPA.beforeFirst();
                    while (rsPA.next()) {
                        List<AMRSPatientAttributes> patientAttributes = amrsPersonAtrributesService.getByPatientIDAndPType(rsPA.getString("person_id"), rsPA.getString("person_attribute_type_id"));
                        if (patientAttributes.size() == 0) {
                            AMRSPatientAttributes apt = new AMRSPatientAttributes();
                            apt.setPatientId(rsPA.getString("person_id"));
                            apt.setPersonAttributeTypeId(rsPA.getString("person_attribute_type_id"));
                            apt.setPersonAttributeName(rsPA.getString("name"));
                            apt.setPersonAttributeValue(rsPA.getString("value"));
                            apt.setKenyaemrAttributeUuid(rsPA.getString("kenyaemruuid"));
                            amrsPersonAtrributesService.save(apt);
                        }

                    }
                    //End of Person Attributes
                    //Person Civilstatus
                    String piddd = rs.getString("person_id");
                    String pstatusSQl = "SELECT  \n" +
                            "                     pa.person_id, \n" +
                            "                    pt.person_attribute_type_id, \n" +
                            "                    case when pt.person_attribute_type_id =5 then '1054' \n" +
                            "                    when pt.person_attribute_type_id =42 then '1542' \n" +
                            "                    when pt.person_attribute_type_id =73 then '1712' \n" +
                            "                    end kenyaemr_concept, \n" +
                            "                    case when pt.person_attribute_type_id =5 then '1054AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    when pt.person_attribute_type_id =42 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    when pt.person_attribute_type_id =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    end kenyaemr_concept_uuid, \n" +
                            "                    pt.name, \n" +
                            "                    cn.name as name_value,\n" +
                            "                    pa.value,\n" +
                            "                    case when pa.value =5555 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    when pa.value =1966 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    when pa.value =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                            "                    when pa.value = 1059 then '1059AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1057 then '1057AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' -- never married / single\n" +
                            "                    when pa.value = 1175 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1056 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1055 then '5555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1060 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5618 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6290 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1670 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 10479 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1058 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value =  8714 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1967 then '1538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 8711 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1968 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1966 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6966 then '159466AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6284 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1971 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6408 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1832 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 8589 then '159465AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6280 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1969 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1970 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6580 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5619 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value =  6401 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1678 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 8407 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 10368 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 10369 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 11283 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1496 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5507 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 8713 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 8710 then  '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 12263 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 12265 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 12262 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 12264 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1602 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1604 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1600 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6216 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6214 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 6215 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1601 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 7583 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 5629 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 1603 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    when pa.value = 7549 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                            "                    end kenyaemr_value_uuid, \n" +
                            "                    pa.date_created  \n" +
                            "                     \n" +
                            "                FROM \n" +
                            "                    amrs.person_attribute pa \n" +
                            "                        INNER JOIN \n" +
                            "                    amrs.person_attribute_type pt ON pa.person_attribute_type_id = pt.person_attribute_type_id and pt.person_attribute_type_id in(42,73,5) \n" +
                            "                    inner join amrs.concept c on c.concept_id = pa.value \n" +
                            "                      inner join amrs.concept_name cn on c.concept_id = cn.concept_id  and cn.locale_preferred=1\n" +
                            "                WHERE \n" +
                            "                       pa.person_id IN (" + piddd + ") \n" +
                            "                        AND  \n" +
                            " pa.voided = 0 order by  pa.person_id  asc";

                    Statement stmtPAStatus = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
                    ResultSet rsPAStatus = stmtPAStatus.executeQuery(pstatusSQl);
                    rsPAStatus.last();
                    x = rsPAStatus.getRow();
                    rsPAStatus.beforeFirst();
                    while (rsPAStatus.next()) {

                        String personId = rsPAStatus.getString("person_id");
                        String personAttributeTypeId = rsPAStatus.getString("person_attribute_type_id");
                        String kenyaEmrConcept = rsPAStatus.getString("kenyaemr_concept");
                        String name = rsPAStatus.getString("name");
                        String value = rsPAStatus.getString("value");
                        String kenyaemr_concept_uuid = rsPAStatus.getString("kenyaemr_concept_uuid");
                        String kenyaemr_value_uuid = rsPAStatus.getString("kenyaemr_value_uuid");
                        String name_value = rsPAStatus.getString("name_value");
                        String patientid = rsPAStatus.getString("person_id");
                        String date_created = rsPAStatus.getString("date_created");
                        List<AMRSPatientStatus> amrsPatientStatusList = amrsPatientStatusService.findByPersonIdAndPersonAttributeTypeId(patientid, personAttributeTypeId);
                        if (amrsPatientStatusList.size() == 0) {
                            AMRSPatientStatus cs = new AMRSPatientStatus();
                            cs.setPersonId(personId);
                            cs.setPersonAttributeTypeId(personAttributeTypeId);
                            cs.setKenyaEmrConcept(kenyaEmrConcept);
                            cs.setName(name);
                            cs.setValue(value);
                            cs.setKenyaEmrConceptUuid(kenyaemr_concept_uuid);
                            cs.setKenyaEmrValueUuid(kenyaemr_value_uuid);
                            cs.setValueName(name_value);
                            // cs.setKenyaPatientUuid(kenyaemrPatientUuid);
                            cs.setObsDateTime(date_created);
                            System.out.println("Tumefika Hapa!!!" + parentUUID);
                          //  amrsPatientStatusService.save(cs);
                        }

                    }
                    //End of Person CivilStatus

                }
                //Migate Patient
                RegisterOpenMRSPayload.patient(ae, amrsPatientServices, amrsIdentifiersService, amrsPersonAtrributesService,amrsPatientStatusService, url, auth);

            }

        }
        con.close();
       }
    public static void patient_relationship(String server, String username, String password, String locations, String parentUUID, AMRSPatientRelationshipService amrsPatientRelationshipService, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

        String samplePatientList = AMRSSamples.getPersonIdList();

        String sql = "SELECT r.person_a,\n" +
                " r.relationship,\n" +
                " rt.a_is_to_b,\n" +
                " rt.b_is_to_a,\n" +
                " rt.uuid,\n" +
                " r.person_b,\n" +
                "case when relationship =1 then '8d919b58-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =2 then '8d91a01c-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =3 then '8d91a210-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =4 then '8d91a3dc-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =5 then '5f115f62-68b7-11e3-94ee-6bef9086de92'\n" +
                "                  when relationship =6 then 'd6895098-5d8d-11e3-94ee-b35a4132a5e3'\n" +
                "                  when relationship =7 then '007b765f-6725-4ae9-afee-9966302bace4'\n" +
                "                  when relationship =8 then '2ac0d501-eadc-4624-b982-563c70035d46'\n" +
                "                  when relationship =9 then '58da0d1e-9c89-42e9-9412-275cef1e0429'\n" +
                "                  when relationship =10 then 'a8058424-5ddf-4ce2-a5ee-6e08d01b5960'\n" +
                "                  end as kenyaemr_uuid\n"+
                " FROM amrs.relationship r \n" +
                " inner join amrs.relationship_type rt on rt.relationship_type_id=r.relationship\n" +
                " where voided =0 and person_a in ("+ samplePatientList +")";

        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(sql);
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {
            String persona =rs.getString("person_a");
            String personb =rs.getString("person_b");
            String relationshipType =rs.getString("relationship");
            String relationshipuuid =rs.getString("uuid");
            String aIsToB =rs.getString("a_is_to_b");
            String bIsToA =rs.getString("b_is_to_a");
            String kenyaemruuid =rs.getString("kenyaemr_uuid");

            System.out.println("aIsToB "+ aIsToB);
            System.out.println("bIsToA "+ bIsToA);

            List<AMRSPatientRelationship> amrsPatientRelationships = amrsPatientRelationshipService.findByPersonAAndPeronBAndRelationship(persona,personb,relationshipType);
            System.out.println("Totals size  "+ amrsPatientRelationships.size());
            if (amrsPatientRelationships.size() == 0) {
                AMRSPatientRelationship pr =new AMRSPatientRelationship();
                System.out.println("aIsToB "+ aIsToB);
                System.out.println("bIsToA "+ bIsToA);
                pr.setAistob(aIsToB);
                pr.setBistoa(bIsToA);
                pr.setPersonA(persona);
                pr.setPersonB(personb);
                pr.setRelationshipUuid(kenyaemruuid);
                pr.setRelationshipType(relationshipType);
               // pr.setRelationshipUuid(relationshipuuid);
                String puuida =   amrsTranslater.KenyaemrPatientUuid(persona);
                String puuidb =   amrsTranslater.KenyaemrPatientUuid(personb);
                System.out.println("bIsToA "+ puuida +" puuida "+ puuidb);
                pr.setKenyaemrpersonAUuid(amrsTranslater.KenyaemrPatientUuid(persona));
                pr.setKenyaemrpersonBUuid(amrsTranslater.KenyaemrPatientUuid(personb));
                amrsPatientRelationshipService.save(pr);

            }

           RegisterOpenMRSPayload.relationship(amrsPatientRelationshipService, url, auth);

        }
    }
}
