#!/usr/bin/env python3
"""
GitHub Actions Build Trigger Script

Triggers and monitors GitHub Actions workflows for building the Stremio APK.

Usage:
    python trigger_build.py --token YOUR_GITHUB_TOKEN [--workflow build-android.yml] [--ref main]
    
Environment variables:
    GITHUB_TOKEN: Personal access token with 'repo' scope
    GITHUB_OWNER: Repository owner (default: iHagoss)
    GITHUB_REPO: Repository name (default: DisFlix)
"""

import argparse
import json
import os
import sys
import time
from urllib import request, error

DEFAULT_OWNER = "iHagoss"
DEFAULT_REPO = "DisFlix"
API_VERSION = "2022-11-28"


def get_headers(token: str) -> dict:
    """Get headers for GitHub API requests."""
    return {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": API_VERSION,
        "Content-Type": "application/json",
    }


def trigger_workflow(token: str, owner: str, repo: str, workflow_id: str, ref: str = "main", inputs: dict = None) -> bool:
    """Trigger a workflow dispatch event."""
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches"
    
    payload = {"ref": ref}
    if inputs:
        payload["inputs"] = inputs
    
    data = json.dumps(payload).encode("utf-8")
    req = request.Request(url, data=data, headers=get_headers(token), method="POST")
    
    try:
        with request.urlopen(req) as response:
            if response.status == 204:
                print(f"Successfully triggered workflow: {workflow_id}")
                return True
    except error.HTTPError as e:
        print(f"Error triggering workflow: {e.code} - {e.reason}")
        body = e.read().decode()
        print(f"Response: {body}")
        return False
    
    return False


def list_workflows(token: str, owner: str, repo: str) -> list:
    """List all workflows in the repository."""
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/workflows"
    req = request.Request(url, headers=get_headers(token))
    
    try:
        with request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            return data.get("workflows", [])
    except error.HTTPError as e:
        print(f"Error listing workflows: {e.code} - {e.reason}")
        return []


def get_latest_run(token: str, owner: str, repo: str, workflow_id: str) -> dict:
    """Get the latest run for a workflow."""
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs?per_page=1"
    req = request.Request(url, headers=get_headers(token))
    
    try:
        with request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            runs = data.get("workflow_runs", [])
            return runs[0] if runs else None
    except error.HTTPError as e:
        print(f"Error getting runs: {e.code} - {e.reason}")
        return None


def wait_for_completion(token: str, owner: str, repo: str, run_id: int, timeout: int = 3600) -> str:
    """Wait for a workflow run to complete."""
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/runs/{run_id}"
    start_time = time.time()
    
    print(f"Waiting for run {run_id} to complete...")
    
    while time.time() - start_time < timeout:
        req = request.Request(url, headers=get_headers(token))
        try:
            with request.urlopen(req) as response:
                data = json.loads(response.read().decode())
                status = data.get("status")
                conclusion = data.get("conclusion")
                
                if status == "completed":
                    print(f"Run completed with conclusion: {conclusion}")
                    return conclusion
                
                print(f"Status: {status}... (elapsed: {int(time.time() - start_time)}s)")
                time.sleep(30)
        except error.HTTPError as e:
            print(f"Error checking status: {e.code}")
            time.sleep(30)
    
    print("Timeout waiting for workflow to complete")
    return "timeout"


def get_artifacts(token: str, owner: str, repo: str, run_id: int) -> list:
    """Get artifacts from a workflow run."""
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/runs/{run_id}/artifacts"
    req = request.Request(url, headers=get_headers(token))
    
    try:
        with request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            return data.get("artifacts", [])
    except error.HTTPError as e:
        print(f"Error getting artifacts: {e.code} - {e.reason}")
        return []


def main():
    parser = argparse.ArgumentParser(description="Trigger GitHub Actions workflow builds")
    parser.add_argument("--token", help="GitHub token (or set GITHUB_TOKEN env var)")
    parser.add_argument("--owner", default=os.getenv("GITHUB_OWNER", DEFAULT_OWNER), help="Repository owner")
    parser.add_argument("--repo", default=os.getenv("GITHUB_REPO", DEFAULT_REPO), help="Repository name")
    parser.add_argument("--workflow", default="build-android.yml", help="Workflow file name")
    parser.add_argument("--ref", default="main", help="Git ref (branch/tag)")
    parser.add_argument("--list", action="store_true", help="List available workflows")
    parser.add_argument("--wait", action="store_true", help="Wait for workflow to complete")
    parser.add_argument("--flavor", choices=["all", "mobile", "tv", "firestick"], help="Build flavor (for build-apk.yml)")
    parser.add_argument("--version", help="Version tag (for release.yml)")
    
    args = parser.parse_args()
    
    token = args.token or os.getenv("GITHUB_TOKEN")
    if not token:
        print("Error: GitHub token is required. Set GITHUB_TOKEN env var or use --token")
        sys.exit(1)
    
    if args.list:
        print(f"\nWorkflows in {args.owner}/{args.repo}:")
        workflows = list_workflows(token, args.owner, args.repo)
        for wf in workflows:
            print(f"  - {wf['name']} ({wf['path'].split('/')[-1]})")
            print(f"    State: {wf['state']}")
        return
    
    inputs = {}
    if args.flavor and args.workflow == "build-apk.yml":
        inputs["flavor"] = args.flavor
    if args.version and args.workflow == "release.yml":
        inputs["version"] = args.version
    
    print(f"\nTriggering workflow: {args.workflow}")
    print(f"Repository: {args.owner}/{args.repo}")
    print(f"Branch/Tag: {args.ref}")
    if inputs:
        print(f"Inputs: {inputs}")
    
    success = trigger_workflow(token, args.owner, args.repo, args.workflow, args.ref, inputs or None)
    
    if success and args.wait:
        time.sleep(5)
        run = get_latest_run(token, args.owner, args.repo, args.workflow)
        if run:
            conclusion = wait_for_completion(token, args.owner, args.repo, run["id"])
            
            if conclusion == "success":
                print("\nArtifacts:")
                artifacts = get_artifacts(token, args.owner, args.repo, run["id"])
                for artifact in artifacts:
                    print(f"  - {artifact['name']} ({artifact['size_in_bytes']} bytes)")
                    print(f"    Download: https://github.com/{args.owner}/{args.repo}/actions/runs/{run['id']}")
                sys.exit(0)
            else:
                sys.exit(1)
    
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
