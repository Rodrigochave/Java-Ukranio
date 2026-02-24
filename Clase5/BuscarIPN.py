import sys
import random
import string

# Verificar argumento
if len(sys.argv) < 2:
    print("Uso: python BuscarIPN.py <n>")
    sys.exit()

n = int(sys.argv[1])

# Generar cadena
palabras = []

for _ in range(n):
    palabra = ''.join(random.choice(string.ascii_uppercase) for _ in range(3))
    palabras.append(palabra)

cadenota = " ".join(palabras) + " "

# Contar ocurrencias de "IPN"
contador = cadenota.count("IPN")


print(contador)
