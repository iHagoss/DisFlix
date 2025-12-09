from flask import Flask, send_from_directory, render_template_string
import os

app = Flask(__name__)

APK_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'output')
APK_FILE = 'DisFlix-debug.zip'

HTML_TEMPLATE = '''
<!DOCTYPE html>
<html>
<head>
    <title>DisFlix APK Download</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            margin: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #fff;
        }
        .container {
            text-align: center;
            padding: 40px;
            background: rgba(255,255,255,0.1);
            border-radius: 20px;
            backdrop-filter: blur(10px);
            max-width: 500px;
        }
        h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .subtitle {
            color: #aaa;
            margin-bottom: 30px;
        }
        .download-btn {
            display: inline-block;
            padding: 15px 40px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 30px;
            font-size: 1.2em;
            font-weight: bold;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .download-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
        }
        .info {
            margin-top: 30px;
            color: #888;
            font-size: 0.9em;
        }
        .version {
            color: #667eea;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>DisFlix</h1>
        <p class="subtitle">Stremio-compatible Android App</p>
        <a href="/download" class="download-btn">Download APK</a>
        <div class="info">
            <p>Version: <span class="version">1.0</span></p>
            <p>Size: 142 MB (zipped)</p>
            <p>Works on Firestick, Android TV, and Mobile</p>
        </div>
    </div>
</body>
</html>
'''

@app.route('/')
def index():
    return render_template_string(HTML_TEMPLATE)

@app.route('/download')
def download():
    return send_from_directory(APK_DIR, APK_FILE, as_attachment=True)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
