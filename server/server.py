from flask import Flask, request, send_from_directory
import json
import mariadb

execution_dir = '/home/enviromon'
# INIT FLASK
app = Flask(__name__, static_folder=f'{execution_dir}/apk')
app.config['JSON_AS_ASCII'] = False #JSONIFY TO UTF8

# INIT MARIADB
conn_params = {
    "user" : "enviromon",
    "password" : "enviromon",
    "host" : "localhost",
    "database" : "enviromon"
}
pool = mariadb.ConnectionPool(pool_name="enviromonpool", pool_size=10, **conn_params)

# ALLOWED DEVICE IDS
devices = ['OFFICE', 'BEDROOM', 'BATHROOM', 'LIVINGROOM', 'HALL', 'CELLAR', 'OUTDOOR']


def authorize(device):
    return device in devices


@app.route('/upgrade', methods=['GET'])
def upgrade():
    # AUTHORIZE
    device = request.headers.get('User-Agent', type = str)[14:]
    if not authorize(device):
        return '', 401
    # GET CLIENT VERSION
    cversion = request.headers.get('User-Agent', type = str)[10:13]
    if cversion is None:
        return '"version" is compulsory parameter representing unique device identifier. required type: int', 400
    # GET SERVER VERSION
    f = open(f'{execution_dir}/apk/.version', 'r')
    sversion = f.read()
    f.close()

    if int(cversion) == int(sversion):
        return '', 204
    elif cversion > sversion:
        print(f'WARNING. Client is higher version than server: {cversion} vs {sversion}')
        return '', 204
    else:
        print(f'attempting to upgrade from version: {cversion} to {sversion}')
        return send_from_directory('/home/skynet/apk', 'skynet.apk')


@app.route('/sensors', methods=['POST'])
def create_sensor():
    # AUTHORIZE
    device = request.headers.get('User-Agent', type = str)[14:]
    if not authorize(device):
        return '', 401

    if not request.is_json:
        return 'body object must pre present and with format: json', 400

    # INSERT NEW DATA
    data = request.json
    db = pool.get_connection();
    cursor = db.cursor()
    cursor.execute(
            "INSERT INTO sensors (device, temperature, pressure, humidity, light, power, battery) VALUES (?,?,?,?,?,?,?)",
            (data['device'], data['temperature'], data['pressure'], data['humidity'], data['light'], data['power'], data['battery'])
    )
    cursor.close()
    db.commit()

    # RESPONSE WITH LAST DATA FROM OUTDOOR SENSOR (TO DISPLAY ON DEVICES)
    cursor = db.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM sensors WHERE device = ? order by creation_date desc limit 1", ['OUTDOOR'])
    data = cursor.fetchall()
    cursor.close()
    db.close()

    # HANDLE RESPONSE
    if data is None:
        return 'Server failed to repond with necessary data', 500
    else:
        response = app.response_class(
            response=json.dumps(data, default=str),
            mimetype='application/json; charset=utf-8'
        )
        return response


@app.route('/sensors', methods=['GET'])
def get_sensors():
    # CHECK PARAM EXISTENCE AND VALUE FORMAT
    try:
        since = request.args.get('since', type = str)
    except ValueError:
        return '"since" parameter has required type: string', 400
    if since is None:
        return '"since" is compulsory parameter representing modification date. required type: string', 400

    print(f'getting sensors since: {since}')

    # ROUTE AND HANDLE REQUEST
    db = pool.get_connection();
    cursor = db.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM sensors WHERE creation_date > ?", [since])
    data = cursor.fetchall()
    cursor.close()
    db.close()

    # HANDLE RESPONSE
    if data is None:
        return 'Server failed to load requested data', 500
    else:
        response = app.response_class(
            response=json.dumps(data, default=str),
            mimetype='application/json; charset=utf-8'
        )
        return response



@app.route('/sensors/last', methods=['GET'])
def get_last_sensors():
    # CHECK PARAM EXISTENCE AND VALUE FORMAT
    try:
        uid = request.args.get('uid', type = str)
    except ValueError:
        return '"uid" parameter has required type: string', 400
    if uid is None:
        return '"uid" is compulsory parameter representing device unique identifier. required type: string', 400

    # ROUTE AND HANDLE REQUEST
    db = pool.get_connection();
    cursor = db.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM sensors WHERE device = ? order by creation_date desc limit 1", [uid])
    data = cursor.fetchall()
    cursor.close()
    db.close()

    # HANDLE RESPONSE
    if data is None:
        return 'Server failed to load requested data', 500
    else:
        response = app.response_class(
            response=json.dumps(data, default=str),
            mimetype='application/json; charset=utf-8'
        )
        return response

application = app

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
