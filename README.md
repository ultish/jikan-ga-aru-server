An exercise in implementing the Timesheet tracker (https://github.com/ultish/jikan-ga-nai-server) using Netflix DGS
framework and Kotlin.

Graphiql: http://localhost:8080/graphiql

## Learning

See re graphql testing and security
https://medium.com/@aleksandar.stojsavljevic/testing-graphql-servers-with-spring-and-dgs-797cccb9f0c7

### Kubernetes

- reuse docker daemon from minikube
    ```zsh
    eval $(minikube docker-env)
    ```
- build docker image
- tag with latest
  ```zsh
  docker tag jikan-ga-aru-server:0.0.1-SNAPSHOT jikan-ga-aru-server
  ```
- generate kube deployment
  ```zsh
  kubectl create deployment jikan-ga-aru-server --image=jikan-ga-aru-server --dry-run -o=yaml > deployment.yaml
  echo --- >> deployment.yaml
  kubectl create service clusterip jikan-ga-aru-server --tcp=8080:8080 --dry-run -o=yaml >> deployment.yaml
  ```
  Note: i modified the tag to include :0.0.1-SNAPSHOT but i did not tag latest either
  Note: i modified the deployment.yaml to set imagePullPolicy: Never
- deploy to kubernetes
  ```zsh
  kubectl apply -f deployment.yaml
  ```
- port forward
  ```zsh
  kubectl port-forward svc/jikan-ga-aru-server 8080:8080
  ```
- unset docker daeomon
    ```zsh
    eval $(minikube docker-env -u)
    ```

### DataLoaders and Custom Contexts

A custom context is passed from parent to child(s) which contains a mutable list of Entities that are loaded within a
request. These are in-turn used to pull relationship information for Many-to-One DataLoaders, so we can perform a
relatively easy query against the DB. Each dataLoader that pulls new data from the DB for the single request will keep
adding Entities loaded into this Custom Context for the next child. (This may assume the child queries are not loaded in
parallel...)

Not sure if this code will survive a refactor to a federated graphQL service in the future though. 
