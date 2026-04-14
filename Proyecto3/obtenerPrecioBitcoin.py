#Proyecto 3        Nombre: Chavez Aquiagual Rodrigo    Grupo:7CM4
import csv
from datetime import datetime, timezone
import os

# Obtener la carpeta donde está este script
ruta_base = os.path.dirname(os.path.abspath(__file__))

# Configuración
archivo_csv = os.path.join(ruta_base, "btcusd_bitstamp_1min_2012-2025.csv")
fecha_deseada = "2023-03-16"  # Fecha deseada a obtener (formato YYYY-MM-DD)
salida = os.path.join(ruta_base, "datos.dat")

# Verificar que el archivo de entrada existe
if not os.path.exists(archivo_csv):
    print(f"ERROR: No se encuentra el archivo CSV en:\n{archivo_csv}")
    exit(1)

# Convertir fecha a timestamps UTC
inicio = int(datetime.strptime(fecha_deseada + " 00:00:00", "%Y-%m-%d %H:%M:%S").replace(tzinfo=timezone.utc).timestamp())
fin = inicio + 24 * 60 * 60  # +24 horas

precios = []

print(f"Extrayendo datos para {fecha_deseada}...")

with open(archivo_csv, 'r') as f:
    reader = csv.reader(f)
    next(reader)  # Saltar encabezado
    for row in reader:
        ts = int(row[0])
        if ts >= fin:
            break
        if ts >= inicio:
            precio = float(row[4])  # columna 'close'
            precios.append(precio)

print(f"Se encontraron {len(precios)} minutos. { '¡Correcto!' if len(precios) == 1440 else 'Faltan minutos.'}")

# Guardar en datos.dat en la misma carpeta
with open(salida, 'w') as f:
    for p in precios:
        f.write(f"{p:.2f}\n")
# Guardar fecha formateada
fecha_obj = datetime.strptime(fecha_deseada, "%Y-%m-%d")
fecha_formateada = fecha_obj.strftime("%d %B %Y")
with open("fecha.txt", "w") as f:
    f.write(fecha_formateada)
print(f"Archivo '{salida}' creado con {len(precios)} líneas.")