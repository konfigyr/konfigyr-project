package com.konfigyr.artifactory.ownership;

import org.mockito.MockedConstruction;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class VerificationStrategyTestUtils {

    public static MockedConstruction<InitialDirContext> mockDns(String domain, String... txtValues) {
        return mockConstruction(InitialDirContext.class, (mock, _) -> {
            final NamingEnumeration<?> enumeration = mock(NamingEnumeration.class);
            final int[] index = {0};
            when(enumeration.hasMore()).thenAnswer(_ -> index[0] < txtValues.length);
            when(enumeration.next()).thenAnswer(_ -> txtValues[index[0]++]);

            final Attribute txtAttr = mock(Attribute.class);
            //noinspection unchecked,rawtypes
            when(txtAttr.getAll()).thenReturn((NamingEnumeration) enumeration);

            final Attributes attrs = mock(Attributes.class);
            when(attrs.get("TXT")).thenReturn(txtAttr);

            when(mock.getAttributes(eq("dns:/" + domain), any(String[].class))).thenReturn(attrs);
        });
    }

    public static MockedConstruction<InitialDirContext> mockDnsNoTxt(String domain) {
        return mockConstruction(InitialDirContext.class, (mock, _) -> {
            final Attributes attrs = mock(Attributes.class);
            when(attrs.get("TXT")).thenReturn(null);
            when(mock.getAttributes(eq("dns:/" + domain), any(String[].class))).thenReturn(attrs);
        });
    }

    public static MockedConstruction<InitialDirContext> mockDnsException(NamingException ex) {
        return mockConstruction(InitialDirContext.class, (mock, _) ->
                when(mock.getAttributes(anyString(), any(String[].class))).thenThrow(ex));
    }
}
