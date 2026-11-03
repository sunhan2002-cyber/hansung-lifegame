import copy
from pathlib import Path
import unittest
import sys

SCRIPTS=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(SCRIPTS))
import validate_followup_image_coverage as check


class CoverageGateTest(unittest.TestCase):
    def setUp(self):
        self.event={'eventId':'e','title':'급식','text':'브로콜리','imageId':'art','choices':[]}
        self.row={key:'' for key in check.INPUT_FIELDS+check.CLASS_FIELDS}
        self.row.update(eventId='e',title='급식',text='브로콜리',preferredImageId='art',classificationOwner='김선한')
        self.base=copy.deepcopy(self.row)
        for key in check.CLASS_FIELDS:self.row[key]='분류'
        self.row.update(proposedGroupId='g',classificationStatus='CLASSIFIED_PENDING_REVIEW')
        self.images=SCRIPTS.parent/'app/src/main/assets/images'
        self.path='week10/scenes/img_path_hap_10_001.png'
        self.routing={'assets':[{'imageId':'art','assetPath':self.path}]}

    def run_audit(self,rows=None,groups=None,events=None):
        return check.audit(events or [self.event],[self.base],rows or [self.row],self.routing,[],self.images,groups)

    def approved(self):
        self.row.update(classificationStatus='APPROVED',approvedGroupId='g',semanticReviewer='의미 검수',visualReviewer='시각 검수',existingArtVerdict='FIT')
        return {'groupId':'g','imageId':'art','assetPath':self.path,'approvalStatus':'APPROVED',
            'semanticReviewer':'의미 검수','visualReviewer':'시각 검수','eventIds':['e'],
            'sha256':check.hashlib.sha256((self.images/self.path).read_bytes()).hexdigest(),
            'eventSourceHashes':{'e':check.event_hash(self.event)}}

    def test_existing_file_is_not_semantic_approval(self):
        report=self.run_audit()
        self.assertEqual(1,report['metrics']['preferredFileEvents'])
        self.assertEqual(0,report['metrics']['approvedSemanticEvents'])
        self.assertEqual(1,len(report['acceptancePending']))

    def test_explicit_review_file_membership_and_fingerprint_are_required(self):
        group=self.approved();report=self.run_audit(groups=[group])
        self.assertEqual([],report['errors']);self.assertEqual([],report['acceptancePending'])
        self.assertEqual(1,report['metrics']['approvedSemanticEvents'])

    def test_missing_file_cannot_pass(self):
        group=self.approved();group['assetPath']='absent.png'
        self.assertTrue(self.run_audit(groups=[group])['errors'])

    def test_duplicate_rows_are_rejected(self):
        self.assertTrue(self.run_audit(rows=[self.row,self.row])['errors'])

    def test_missing_event_is_rejected(self):
        self.assertTrue(self.run_audit(events=[self.event,dict(self.event,eventId='missing')])['errors'])

    def test_original_text_cannot_be_silently_changed(self):
        self.row['text']='다른 사건'
        self.assertTrue(self.run_audit()['errors'])

    def test_shared_group_requires_each_event_membership(self):
        group=self.approved();group['eventIds']=['someone_else']
        self.assertTrue(self.run_audit(groups=[group])['acceptancePending'])

    def test_source_change_invalidates_approval(self):
        group=self.approved();group['eventSourceHashes']['e']='outdated'
        self.assertTrue(self.run_audit(groups=[group])['acceptancePending'])

    def test_path_traversal_and_unsupported_group_are_rejected(self):
        group=self.approved();group['assetPath']='../outside.png'
        self.assertTrue(self.run_audit(groups=[group])['errors'])

    def test_revised_art_requires_resolution_evidence(self):
        group=self.approved();self.row['existingArtVerdict']='REVISE_REQUIRED'
        self.assertTrue(self.run_audit(groups=[group])['acceptancePending'])
        group['resolvedIssueEvidence']={'e':'revision-review.md'}
        self.assertEqual([],self.run_audit(groups=[group])['acceptancePending'])

    def test_generic_fallback_never_counts_as_completed_event_art(self):
        group=self.approved();self.routing['statScenes']={'health':'art'}
        report=self.run_audit(groups=[group])
        self.assertTrue(report['errors'])
        self.assertEqual(0,report['metrics']['approvedSemanticEvents'])

    def test_image_change_invalidates_previous_approval(self):
        group=self.approved();group['sha256']='old-image'
        self.assertTrue(self.run_audit(groups=[group])['errors'])
