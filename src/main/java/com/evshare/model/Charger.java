package com.evshare.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
@Entity @Table(name="chargers")
public class Charger {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank private String title;
 @NotBlank private String hostName;
 @NotBlank private String address;
 private String city;
 @NotBlank private String chargerType;
 @NotNull @Positive private BigDecimal powerKw;
 @NotNull @DecimalMin("10.0") @DecimalMax("20.0") private BigDecimal pricePerKwh;
 private String connectorType="Type 2";
 private String availableFrom="08:00";
 private String availableTo="22:00";
 private boolean active=true;
 private double latitude; private double longitude;
 private String manufacturer; private String model; private String conditionGrade="GOOD";
 private String ipRating; private String safetyCertification; private String warrantyStatus;
 private Integer installationYear; private String lastInspectionDate; private String voltage; private String phase;
 private String currentRating; private String smartFeatures; private String qualityNotes;
 private boolean hostVerified=false; private boolean safetyVerified=false;
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getHostName(){return hostName;} public void setHostName(String v){hostName=v;}
 public String getAddress(){return address;} public void setAddress(String v){address=v;}
 public String getCity(){return city;} public void setCity(String v){city=v;}
 public String getChargerType(){return chargerType;} public void setChargerType(String v){chargerType=v;}
 public BigDecimal getPowerKw(){return powerKw;} public void setPowerKw(BigDecimal v){powerKw=v;}
 public BigDecimal getPricePerKwh(){return pricePerKwh;} public void setPricePerKwh(BigDecimal v){pricePerKwh=v;}
 public String getConnectorType(){return connectorType;} public void setConnectorType(String v){connectorType=v;}
 public String getAvailableFrom(){return availableFrom;} public void setAvailableFrom(String v){availableFrom=v;}
 public String getAvailableTo(){return availableTo;} public void setAvailableTo(String v){availableTo=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public double getLatitude(){return latitude;} public void setLatitude(double v){latitude=v;}
 public double getLongitude(){return longitude;} public void setLongitude(double v){longitude=v;}
 public String getManufacturer(){return manufacturer;} public void setManufacturer(String v){manufacturer=v;}
 public String getModel(){return model;} public void setModel(String v){model=v;}
 public String getConditionGrade(){return conditionGrade;} public void setConditionGrade(String v){conditionGrade=v;}
 public String getIpRating(){return ipRating;} public void setIpRating(String v){ipRating=v;}
 public String getSafetyCertification(){return safetyCertification;} public void setSafetyCertification(String v){safetyCertification=v;}
 public String getWarrantyStatus(){return warrantyStatus;} public void setWarrantyStatus(String v){warrantyStatus=v;}
 public Integer getInstallationYear(){return installationYear;} public void setInstallationYear(Integer v){installationYear=v;}
 public String getLastInspectionDate(){return lastInspectionDate;} public void setLastInspectionDate(String v){lastInspectionDate=v;}
 public String getVoltage(){return voltage;} public void setVoltage(String v){voltage=v;}
 public String getPhase(){return phase;} public void setPhase(String v){phase=v;}
 public String getCurrentRating(){return currentRating;} public void setCurrentRating(String v){currentRating=v;}
 public String getSmartFeatures(){return smartFeatures;} public void setSmartFeatures(String v){smartFeatures=v;}
 public String getQualityNotes(){return qualityNotes;} public void setQualityNotes(String v){qualityNotes=v;}
 public boolean isHostVerified(){return hostVerified;} public void setHostVerified(boolean v){hostVerified=v;}
 public boolean isSafetyVerified(){return safetyVerified;} public void setSafetyVerified(boolean v){safetyVerified=v;}
}
