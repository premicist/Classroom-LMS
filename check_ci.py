import urllib.request
import json
import sys

headers = {
    'Accept': 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28'
}

def fetch(url):
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())

# Fetch runs for the Android CI workflow
data = fetch('https://api.github.com/repos/premicist/Classroom-LMS/actions/workflows/377700396/runs?per_page=10')
runs = data.get('workflow_runs', [])

print("All Recent Runs:")
print(f"{'Display Title':<50} {'Status':<12} {'Result':<12} {'URL'}")
print("-" * 100)
for r in runs:
    title = (r.get('display_title') or r.get('name', ''))[:48]
    status = r.get('status', '?')
    conclusion = r.get('conclusion', '?')
    url = r['html_url']
    print(title + " | " + status + " | " + conclusion + " | " + url)

# Jobs to check
TARGET_JOBS = ['Unit Tests', 'Build Debug APK', 'Build Release APK']

if runs:
    latest_run = runs[0]
    run_id = latest_run['id']
    
    print("")
    print("=" * 100)
    print("Latest Run #" + str(latest_run['run_number']) + ": " + latest_run['display_title'])
    print("SHA: " + latest_run['head_sha'])
    print("=" * 100)
    
    jobs_data = fetch('https://api.github.com/repos/premicist/Classroom-LMS/actions/runs/' + str(run_id) + '/jobs')
    jobs = jobs_data.get('jobs', [])
    
    for target in TARGET_JOBS:
        matching = [j for j in jobs if j['name'] == target]
        if not matching:
            matching = [j for j in jobs if target.lower() in j['name'].lower()]
        
        print("")
        print("--- " + target + " ---")
        for j in matching:
            status = j.get('status', '?')
            conclusion = j.get('conclusion', '?')
            job_url = j['html_url']
            icon = 'OK' if conclusion == 'success' else ('FAIL' if conclusion == 'failure' else 'PENDING')
            print("  Status: " + status + " | Result: " + conclusion + " | " + icon)
            print("  URL: " + job_url)
            
            steps = j.get('steps', [])
            failed_steps = [s for s in steps if s.get('conclusion') == 'failure']
            if failed_steps:
                print("  Failed Steps:")
                for s in failed_steps:
                    print("    - " + s['name'] + ": " + s.get('status', '?'))
                    if s.get('completed_at'):
                        print("      Completed at: " + s['completed_at'])
            else:
                print("  All steps passed.")
            
            # Check for job-level annotations
            annotations = j.get('annotations', [])
            if annotations:
                print("  Annotations:")
                for a in annotations[:10]:
                    msg = a.get('message', '')
                    if msg:
                        print("    [" + a.get('level', '?') + "] " + msg[:200])
            
            # Fetch full job detail for richer annotation info
            try:
                job_detail = fetch(j['url'])
                anns = job_detail.get('annotations', [])
                if anns and not annotations:
                    print("  Annotations (from detail):")
                    for a in anns[:10]:
                        msg = a.get('message', '')
                        if msg:
                            print("    [" + a.get('level', '?') + "] " + msg[:300])
            except Exception as e:
                pass



