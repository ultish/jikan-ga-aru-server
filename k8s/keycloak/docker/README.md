# Keycloak https

1. Create realm
2. Realm Settings
    1. Unmanaged Attributes: Enabled
3. Create Client
    1. Settings:
        1. Set clientID
        2. Set Valid redirect URI: http://localhost:4200/*
        3. Web Origins: *
        4. Client Authentication: off
        5. Authorization: off
        6. Standard flow: yes
        7. Implicit flow: yes
        8. Direct Access Grants: yes?
    2. Client Scopes:
        1. Root-dedicated scope:
            1. Add Mapper by configuration 
                1. User Attribute
                    1. name: x509-dn-mapper
                    2. User attribute: x509_dn
                    3. Token Claim Name: x509_dn
                    4. Add to ID token
                    5. Add to access token
4. Create User:
    1. username: root
    2. Attributes:
        1. Key: x509_dn, value: CN=root, O=jxhui, ST=Some-State, C=AU
5. Authentication:
    1. Duplicate browser flow
        1. delete everything except X509/Validate Username Form, configure it:
            1. User Identity Source: Match SubjectDN using regular expression
            2. Canonical DN rep enabled: off
            3. enable serial number hex rep: off
            4. a regular epxression to extract user identy: ^(.*)$
            5. User mapping method: Custom Attribute Mapper
            6. A name of user attribute: x509_dn
            7. Check cert validity: on
