from flask import Flask, request, jsonify
from pymongo import MongoClient
import gridfs
import face_recognition
import numpy as np
import io
from PIL import Image

app = Flask(__name__)

# Conectar a MongoDB
client = MongoClient('mongodb://localhost:27017/')
db = client['face_database']
fs = gridfs.GridFS(db)
people_collection = db['people']

@app.route('/add_and_validate_person', methods=['POST'])
def add_and_validate_person():
    try:
        name = request.form['name']
        image_file = request.files['image']
        image = Image.open(image_file)
        image = np.array(image)

        encodings = face_recognition.face_encodings(image)
        
        if not encodings:
            return jsonify({"error": "No se encontraron codificaciones faciales en la imagen."})
        
        test_encoding = encodings[0]

        # Busca en la base de datos
        people = people_collection.find()
        found = False
        for person in people:
            known_encoding = np.array(person['encoding'])
            results = face_recognition.compare_faces([known_encoding], test_encoding)
            if results[0]:
                found = True
                break

        return jsonify({
            "message": "Persona encontrada en la base de datos." if found else "Persona no encontrada en la base de datos.",
            "found": found
        })

    except Exception as e:
        return jsonify({"error": f'Ocurrió un error: {e}'})



if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
