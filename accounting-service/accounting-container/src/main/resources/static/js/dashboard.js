/**
 * Dashboard interactivity: post/reverse journal entries via API, etc.
 */
(function () {
  'use strict';

  window.dashboard = {
    postJournalEntry: function (entryId, successUrl) {
      fetch('/api/v1/journal-entries/' + entryId + '/post', { method: 'POST', headers: { 'Accept': 'application/json' } })
        .then(function (r) {
          if (r.ok) {
            if (successUrl) window.location.href = successUrl;
            else window.location.reload();
          } else {
            return r.json().then(function (body) { throw new Error(body.message || 'Post failed'); });
          }
        })
        .catch(function (e) {
          alert(e.message || 'Failed to post entry');
        });
    },
    reverseJournalEntry: function (entryId, successUrl) {
      fetch('/api/v1/journal-entries/' + entryId + '/reverse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ reason: 'Reversal from dashboard' })
      })
        .then(function (r) {
          if (r.ok) {
            if (successUrl) window.location.href = successUrl;
            else window.location.reload();
          } else {
            return r.json().then(function (body) { throw new Error(body.message || 'Reverse failed'); });
          }
        })
        .catch(function (e) {
          alert(e.message || 'Failed to reverse entry');
        });
    }
  };
})();
