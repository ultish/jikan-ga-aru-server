this covers installing keycloak with istio

1. install keycloak
   ```zsh
   helm install keycloak oci://registry-1.docker.io/bitnamicharts/keycloak --values keycloak/values.yaml
   ```
2. visit http://<gateway ip eg 192.168.1.88>/keycloak/admin/master/console/
3. 