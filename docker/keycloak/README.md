# Docker

- Had to 777 all the certs folders and postgres_data folder

# Keycloak https

Access: https://localhost:18443/

1. Create realm (jxhui)
2. Realm Settings
    1. Unmanaged Attributes: Enabled
3. Create Client
    1. Settings:
        1. Set clientID (jxhui)
        2. Set Valid redirect URI: http://localhost:4200/*
        3. Web Origins: *
        4. Client Authentication: off
        5. Authorization: off
        6. Standard flow: yes
        7. Implicit flow: yes
        8. Direct Access Grants: yes? <not using this for now>
    2. Client Scopes:
        1. Root-dedicated scope (select your <realm>-dedicated scope:
            1. Add Mapper via "Configure a new Mapper/By Configuration"
                1. Select "User Attribute" from list
                    1. name: x509-dn-mapper
                    2. User attribute: x509_dn
                    3. Token Claim Name: x509_dn
                    4. Add to ID token
                    5. Add to access token
                       2, Add Mapper via "Configure a new Mapper"
            2. Add Mapper via "From predefined mappers"
                1. Select client roles
                2. Click back into client roles
                3. set the Client ID to jxhui
                4. change the token claim name if you want (eg roles). defaults to
                   resource_access.${client_id}.roles
                5. set Add to ID token to On

4. Create Roles
    1. Realm Roles > Create Role, create "tester"
5. Create User:
    1. username: root
    2. Attributes:
        1. Key: x509_dn, value: CN=root, O=jxhui, ST=Some-State, C=AU
    3. Role Mapping, Assign role "tester"
6. Authentication:
    1. Duplicate browser flow and name it x509
        1. delete everything except X509/Validate Username Form, configure it:
            1. User Identity Source: Match SubjectDN using regular expression
            2. Canonical DN rep enabled: off
            3. enable serial number hex rep: off
            4. a regular epxression to extract user identy: ^(.*)$
            5. User mapping method: Custom Attribute Mapper
            6. A name of user attribute: x509_dn
            7. Check cert validity: on
        2. in Action drop down click Bind flow and select Browser flow
           See https://www.keycloak.org/docs/latest/server_admin/index.html#_x509 for more details
7. Realm Settings:
    1. Tokens, set Access Token Lifespan
    2. also look at session tab for settings 