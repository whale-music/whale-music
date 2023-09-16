package org.api.subsonic.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.core.utils.SerializeUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JacksonXmlRootElement(localName = "subsonic-response")
@JsonRootName("subsonic-response")
public class SubsonicResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    @JacksonXmlProperty(isAttribute = true)
    private String xmlns = "http://subsonic.org/restapi";
    
    @JacksonXmlProperty(isAttribute = true)
    private String status = "ok";
    
    @JacksonXmlProperty(isAttribute = true)
    private String version = "1.16.1";
    
    @JacksonXmlProperty(isAttribute = true)
    private String type = "whale";
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Error error;
    
    public ResponseEntity<String> success(SubsonicCommonReq req) {
        this.status = "ok";
        String json = req.getF();
        boolean isJson = StringUtils.equalsIgnoreCase(json, "json");
        return getResponseEntity(isJson);
    }
    
    public ResponseEntity<String> error(SubsonicCommonReq req, ErrorEnum error) {
        this.status = "failed";
        this.error = error.error();
        String json = req.getF();
        boolean isJson = StringUtils.equalsIgnoreCase(json, "json");
        return getResponseEntity(isJson);
    }
    
    public ResponseEntity<String> error(boolean isJson, ErrorEnum error) {
        this.status = "failed";
        this.error = error.error();
        return getResponseEntity(isJson);
    }
    
    @NotNull
    private ResponseEntity<String> getResponseEntity(boolean isJson) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (isJson) {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        } else {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
        }
        return new ResponseEntity<>(SerializeUtil.serialize(this, isJson),
                httpHeaders,
                HttpStatus.OK);
    }
    
}
