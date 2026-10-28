import base64
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import serialization
import os

# 1. Generate RSA Key Pair (1024 bits for short codes)
private_key = rsa.generate_private_key(public_exponent=65537, key_size=1024)
public_key = private_key.public_key()

# 2. Export Private Key to PEM format
private_pem = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.TraditionalOpenSSL,
    encryption_algorithm=serialization.NoEncryption()
).decode('utf-8')

# 3. Export Public Key to Base64 string
public_der = public_key.public_bytes(
    encoding=serialization.Encoding.DER,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)
public_base64 = base64.b64encode(public_der).decode('utf-8')

# 4. Write Private Key to file
with open("admin_private_key.txt", "w") as f:
    f.write(private_pem)

# 5. Write Public Key into strings.xml
xml_content = f'''<resources>
    <string name="app_name">NovaMpesa Tracker</string>
    <string name="public_key">{public_base64}</string>
</resources>'''
with open("app/src/main/res/values/strings.xml", "w") as f:
    f.write(xml_content)

print("SUCCESS: Keys generated and strings.xml updated!")
