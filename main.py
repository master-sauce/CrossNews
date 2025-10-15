from flask import Flask, request, render_template_string
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin
import json

app = Flask(_name_)

DOMAINS = [
    {'name': '14', 'url': 'https://www.c14.co.il'},
    {'name': '13', 'url': 'https://13tv.co.il/news'},
    {'name': '12', 'url': 'https://www.ynet.co.il/news'},
    {'name': '11', 'url': 'https://www.kan.org.il/lobby/news'}
]

def fetch_page_content(url):
    """Fetch the HTML content of the page."""
    try:
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'}
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        return response.text
    except Exception as e:
        return None

def fetch_channel_14_api(keyword):
    """Fetch articles from Channel 14 API and search for keyword in title and description."""
    try:
        api_url = 'https://www.c14.co.il/wp-json/now14-api/v1/articles/'
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'}
        response = requests.get(api_url, headers=headers, timeout=10)
        response.raise_for_status()
        articles = response.json()
        
        keyword_lower = keyword.lower()
        matching_results = []
        
        for article in articles:
            title = article.get('title', '').lower()
            description = article.get('seo', {}).get('description', '').lower()
            article_id = article.get('id', None)
            if article_id and (keyword_lower in title or keyword_lower in description):
                built_url = f"https://www.c14.co.il/article/{article_id}"
                if built_url not in [result['url'] for result in matching_results]:  # Avoid duplicates
                    matching_results.append({'url': built_url, 'title': article.get('title', '')})
        
        return matching_results
    except Exception as e:
        return []

def fetch_channel_11_api(keyword):
    """Fetch articles from Channel 11 API and search for keyword in title and description."""
    try:
        api_url = 'https://heyday.io/search/s/'
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'}
        payload = {
            "affId": 2543,
            "q": keyword,
            "h": "kan.org.il",
            "p": 4,
            "filters": {"categories": [["news", True]]}
        }
        response = requests.post(api_url, json=payload, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()
        
        matching_results = []
        for item in data.get('r', []):
            pd = item.get('pd', {})
            title = pd.get('title', '').lower()
            description = pd.get('description', '').lower()
            link = pd.get('url', '')
            keyword_lower = keyword.lower()
            if link and (keyword_lower in title or keyword_lower in description):
                if link not in [result['url'] for result in matching_results]:  # Avoid duplicates
                    matching_results.append({'url': link, 'title': pd.get('title', '')})
        
        return matching_results
    except Exception as e:
        return []

def parse_and_search(html_content, keyword, base_url):
    """Parse HTML and search for keyword in text, return all hrefs found."""
    if not html_content:
        return []
    
    soup = BeautifulSoup(html_content, 'html.parser')
    keyword_lower = keyword.lower()
    matching_urls = []
    
    links = soup.find_all('a', href=True)
    for link in links:
        text = link.get_text(strip=True).lower()
        if keyword_lower in text:
            href = link['href']
            full_url = urljoin(base_url, href) if not href.startswith('http') else href
            if full_url not in [result['url'] for result in matching_urls]:  # Avoid duplicates
                matching_urls.append({'url': full_url, 'title': ''})
    
    return matching_urls

@app.route('/', methods=['GET', 'POST'])
def index():
    results = []
    keyword = ''
    
    if request.method == 'POST':
        keyword = request.form.get('keyword', '').strip()
        if keyword:
            for domain_info in DOMAINS:
                domain_name = domain_info['name']
                domain_url = domain_info['url']
                if domain_name == '14':
                    # Special handling for Channel 14 using API
                    links = fetch_channel_14_api(keyword)
                elif domain_name == '11':
                    # Special handling for Channel 11 using API
                    links = fetch_channel_11_api(keyword)
                else:
                    # Standard HTML parsing for other channels (13, 12)
                    html = fetch_page_content(domain_url)
                    links = parse_and_search(html, keyword, domain_url)
                results.append({
                    'name': domain_name,
                    'url': domain_url,
                    'results': links if links else [{'url': 'לא נמצאה התאמה', 'title': ''}]
                })
    
    return render_template_string('''
        <!DOCTYPE html>
        <html lang="he" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>חיפוש חדשות</title>
            <script src="https://cdn.tailwindcss.com"></script>
            <style>
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .fade-in {
                    animation: fadeIn 0.5s ease-out;
                }
            </style>
        </head>
        <body class="bg-gray-100 min-h-screen flex flex-col items-center justify-center">
            <div class="max-w-3xl w-full p-6">
                <h1 class="text-3xl font-bold text-gray-800 text-center mb-8">חיפוש כתבות לפי מילת מפתח</h1>
                
                <form method="post" class="flex justify-center mb-8">
                    <input type="text" name="keyword" placeholder="הזן מילת מפתח (למשל: ביבי)" 
                           value="{{ keyword }}"
                           class="p-3 w-80 text-lg rounded-r-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500">
                    <button type="submit" 
                            class="p-3 bg-blue-600 text-white text-lg rounded-l-lg hover:bg-blue-700 transition-colors">
                        חפש
                    </button>
                </form>

                {% if results %}
                    <div class="space-y-6">
                        {% for result in results %}
                            <div class="bg-white p-6 rounded-lg shadow-md fade-in">
                                <h3 class="text-xl font-semibold text-blue-600 mb-2">
                                    {{ result.name }} (ערוץ {{ result.name }})
                                </h3>
                                <p class="text-gray-700">
                                    כתובת: <a href="{{ result.url }}" target="_blank" 
                                              class="text-blue-500 hover:underline">{{ result.url }}</a>
                                </p>
                                <p class="text-gray-700">תוצאות:</p>
                                <ul class="list-disc mr-5">
                                    {% for link in result.results %}
                                        {% if link.url.startswith('http') %}
                                            <li>
                                                <a href="{{ link.url }}" target="_blank" 
                                                   class="text-blue-500 hover:underline">{{ link.url }}</a>
                                                {% if link.title %}
                                                    <span class="text-gray-600"> - {{ link.title }}</span>
                                                {% endif %}
                                            </li>
                                        {% else %}
                                            <li><span class="text-gray-500">{{ link.url }}</span></li>
                                        {% endif %}
                                    {% endfor %}
                                </ul>
                            </div>
                        {% endfor %}
                    </div>
                {% endif %}
            </div>
        </body>
        </html>
    ''', results=results, keyword=keyword)

if _name_ == '_main_':
    app.run(debug=True)
