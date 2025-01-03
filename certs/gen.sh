#!/bin/bash

# Prompt the user for inputs
read -p "Enter Common Name (CN, e.g., *.example.com): " commonName
read -p "Enter the base filename (e.g., mydomain): " baseFilename
read -p "Enter the CA certificate file path: " caCert
read -p "Enter the CA private key file path: " caKey
read -p "Enter the number of days the certificate should be valid: " validityDays

# Prompt for additional SAN entries (DNS)
echo "Enter additional DNS SANs (one per line, leave blank to finish):"
dns_sans=()
while true; do
  read -p "DNS SAN (e.g., sub.example.com): " dns_san
  [[ -z "$dns_san" ]] && break
  dns_sans+=("$dns_san")
done

# Prompt for additional SAN entries (IP)
echo "Enter additional IP SANs (one per line, leave blank to finish):"
ip_sans=()
while true; do
  read -p "IP SAN (e.g., 192.168.1.1): " ip_san
  [[ -z "$ip_san" ]] && break
  ip_sans+=("$ip_san")
done

# Generate private key
openssl genrsa -out "$baseFilename.key" 2048

echo "Creating OpenSSL configuration file..."

# Create the openssl.cnf configuration file
cat <<EOL > openssl.cnf
[req]
default_md = sha256
prompt = no
req_extensions = req_ext
distinguished_name = req_distinguished_name

[req_distinguished_name]
commonName = $commonName
countryName = US
stateOrProvinceName = No state
localityName = City
organizationName = LTD

[req_ext]
keyUsage = critical,digitalSignature,keyEncipherment
extendedKeyUsage = critical,serverAuth,clientAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${commonName#*.}
DNS.2 = $commonName
EOL

# Append additional DNS SANs to the config file
counter=3
for dns_san in "${dns_sans[@]}"; do
  echo "DNS.$counter = $dns_san" >> openssl.cnf
  ((counter++))
done

# Append additional IP SANs to the config file
for ip_san in "${ip_sans[@]}"; do
  echo "IP.$counter = $ip_san" >> openssl.cnf
  ((counter++))
done

# Create the CSR
openssl req -new -nodes -key "$baseFilename.key" -config openssl.cnf -out "$baseFilename.csr"

# Verify the CSR
openssl req -noout -text -in "$baseFilename.csr"

# Sign the certificate
openssl x509 -req -in "$baseFilename.csr" -CA "$caCert" -CAkey "$caKey" -CAcreateserial -out "$baseFilename.crt" -days "$validityDays" -sha256 -extfile openssl.cnf -extensions req_ext

echo "Certificate and key generated successfully:"
echo "- Private Key: $baseFilename.key"
echo "- CSR: $baseFilename.csr"
echo "- Certificate: $baseFilename.crt"
