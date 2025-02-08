this covers installing keycloak with istio

1. install keycloak
   ```zsh
   helm install keycloak oci://registry-1.docker.io/bitnamicharts/keycloak --values keycloak/values.yaml
   ```
   if you want to see all values.yml visit https://github.com/bitnami/charts/blob/main/bitnami/keycloak/values.yaml
2. visit http://<gateway ip eg 192.168.1.88>/keycloak/admin/master/console/
3. 