package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VcsProviderInfo {

    private String provider;

    private String urlPattern;

    private String exampleUrl;

    private String tokenField;

    private String alternateTokenField;

    private boolean publicAccessSupported;

    private String publicRequestExample;

    private String privateRequestExample;
}
