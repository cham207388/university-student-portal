# Reference Project Data

<details>
<summary>DynamoDB</summary>

| Name | Purpose | Terraform source |
|------|---------|------------------|
| `dev-AddressExtract` | Address extraction results | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-Audit` | Submission audit trail | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-ClientInformation` | Client information per submission | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-CoreMetaData` | Core application metadata | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-EvidenceMetaData` | Evidence upload metadata | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-FeeWaiverMetaData` | I-912 fee waiver metadata | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-FormMetaData` | Form definitions and configuration | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-PaygovTransaction` | PayGov mock transactions (dev tools) | `tools/test-doubles/paygov/devops/terraform/app/dynamodb.tf` |
| `dev-Submission` | Primary submission records | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-SubmissionManifest` | Lockbox submission manifests | `services/lockbox-gateway/devops/terraform/app/dynamodb.tf` |
| `dev-SubmissionSummary` | Submission summaries | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-TextractJob` | Textract job tracking | `devops/platform/infrastructure/dynamodb.tf` |
| `dev-ValidationStatus` | Form validation status | `devops/platform/infrastructure/dynamodb.tf` |

All tables: `PAY_PER_REQUEST`, streams enabled, SSE enabled, deletion protection off for dev.

**JSON Tables structure**

<details>
<summary>dev-AddressExtract</summary>

```json
{
  "_table": "dev-AddressExtract",
  "_keys": {
    "partition": "submissionId"
  },
  "_indexes": {},
  "_source": {
    "terraform": "devops/platform/infrastructure/dynamodb.tf",
    "javaBinding": "No Java @DynamoDbBean found for AddressExtract",
    "localInit": "services/submission-service/init-aws-local.sh"
  },
  "item": {
    "submissionId": "string",
    "_payload": "unmodeled address extraction attributes if present in the live table"
  },
  "_migrationNotes": [
    "Terraform provisions AddressExtract with only submissionId modeled as a key.",
    "Java binds a sibling local-only NameExtract table instead of AddressExtract; verify live data before ETL."
  ]
}
```

</details>

---

<details>
<summary>dev-Audit</summary>

```json
{
  "_table": "dev-Audit",
  "_keys": {
    "partition": "myUscisId",
    "sort": "submissionId"
  },
  "_indexes": {},
  "item": {
    "myUscisId": "string",
    "submissionId": "string",
    "events": [
      {
        "timestamp": "instant",
        "actionPerformed": "enum: DRAFT_CREATED | SELECT_ELIGIBILITY_CATEGORY | SELECT_IMMVI | SELECT_REASON_FOR_FILING | REVIEW_SIGNATURE | FEE_WAIVER_UPLOADED | FEE_WAIVER_REQUESTED | FORM_UPLOADED | FORM_DELETED | EVIDENCE_UPLOADED | EVIDENCE_DELETED_WITHOUT_S3 | EVIDENCE_DELETED_WITH_S3 | SIGNATURE_DETECTED | SIGNATURE_NOT_DETECTED | SUBMISSION_UPDATED | SUBMISSION_REPLAYED | MANIFEST_SENT | PAYMENT_REQUEST_RECEIVED | PAYMENT_RESPONSE_SENT | SELECT_C9_OPTION | MD5HASH_CREATED | FORMTYPE_UPDATED | EVIDENCE_TYPE_ID_UPDATED",
        "submissionState": "enum: DRAFT | DELETED | PUBLISHED | ACCEPTED | REJECTED | RECEIVED | ERROR",
        "metadata": "string"
      }
    ]
  }
}
```

</details>

---

<details>
<summary>dev-ClientInformation</summary>

```json
{
  "_table": "dev-ClientInformation",
  "_keys": {
    "partition": "submissionId"
  },
  "_indexes": {
    "by_clientId": {
      "partition": "clientId",
      "projection": "ALL"
    }
  },
  "item": {
    "submissionId": "string",
    "clientId": "string",
    "attorneyId": "string",
    "name": {
      "firstName": "string",
      "middleName": "string",
      "lastName": "string",
      "organizationName": "string"
    },
    "userGroupIds": [
      "string"
    ]
  },
  "_relationship": "Representative-filed submissions reference this table by submissionId. Submission.clientInformation is @DynamoDbIgnore and is populated from this table at read time."
}
```

</details>

---

<details>
<summary>dev-CoreMetaData</summary>

```json
{
  "_table": "dev-CoreMetaData",
  "_keys": {
    "partition": "id"
  },
  "_indexes": {},
  "item": {
    "id": "string",
    "paygovMaintenanceWindow": {
      "start": "zonedDateTime",
      "end": "zonedDateTime"
    },
    "betaBanners": [
      {
        "bannerText": "string",
        "formType": "string",
        "showBanner": "boolean",
        "showBannerFor": "enum: APPLICANT | REPRESENTATIVE | REGISTRANT"
      }
    ]
  }
}
```

</details>

---

<details>
<summary>dev-EvidenceMetaData</summary>

```json
{
  "_table": "dev-EvidenceMetaData",
  "_keys": {
    "partition": "id"
  },
  "_indexes": {},
  "item": {
    "id": "string",
    "evidencePages": {
      "evidencePageId": {
        "component": "string",
        "dropzoneProps": {
          "maxSize": "integer",
          "maxUploads": "integer",
          "maxDrop": "integer",
          "validated": "boolean",
          "accept": {
            "mime/type": [
              "file-extension-or-media-subtype"
            ]
          }
        },
        "evidenceWarnings": [
          {
            "variety": "string",
            "headerText": "string",
            "bodyText": "string"
          }
        ]
      }
    }
  }
}
```

</details>

---

<details>
<summary>dev-FeeWaiverMetaData</summary>

```json
{
  "_table": "dev-FeeWaiverMetaData",
  "_keys": {
    "partition": "formType"
  },
  "_indexes": {},
  "item": {
    "formType": "string",
    "formDescription": "string",
    "editions": [
      {
        "edition": "string",
        "expires": "string",
        "numberOfPages": "integer",
        "signaturePage": "integer"
      }
    ]
  }
}
```

</details>

---

<details>
<summary>dev-FormMetaData</summary>

```json
{
  "_table": "dev-FormMetaData",
  "_keys": {
    "partition": "formType"
  },
  "_indexes": {},
  "item": {
    "formType": "string",
    "formDescription": "string",
    "showBanner": "boolean",
    "enabled": "boolean",
    "feeWaiverEnabled": "boolean",
    "elisEsignFlowEnabled": "boolean",
    "currentEdition": "string",
    "representativeEnabled": "boolean",
    "signaturePages": [
      {
        "entity": "enum: APPLICANT | REPRESENTATIVE | REGISTRANT",
        "page": "integer"
      }
    ],
    "editions": [
      {
        "numberOfPages": "integer",
        "edition": "string",
        "ombNo": "string",
        "expires": "string",
        "available": "string"
      }
    ],
    "validations": [
      "enum: CORE | SIGNATURE | FORM_FIELDS | NOA | G28_COMP | G28_COMP_NAME | G28_NAME_EMAIL | G28_ATTORNEY_NAME | PFVS | I864 | I864EZ | N648"
    ],
    "eligibilityCategories": [
      {
        "category": "string",
        "label": "string",
        "code": "integer",
        "benefitTypeCode": "string",
        "applicationReasonCode": "string",
        "metaData": {
          "feeWaivable": "boolean",
          "feeExemptable": "boolean",
          "evidenceGroups": [
            {
              "id": "string",
              "name": "string",
              "evidenceTypes": [
                {
                  "id": "string",
                  "name": "string",
                  "contentCategoryCode": "integer"
                }
              ],
              "validations": [
                "enum: CORE | SIGNATURE | FORM_FIELDS | NOA | G28_COMP | G28_COMP_NAME | G28_NAME_EMAIL | G28_ATTORNEY_NAME | PFVS | I864 | I864EZ | N648"
              ],
              "evidenceWarnings": [
                {
                  "variety": "string",
                  "headerText": "string",
                  "bodyText": "string"
                }
              ],
              "maxNumberOfFiles": "integer",
              "allowMediaTypes": [
                "mime/type"
              ]
            }
          ],
          "feeExemptQuestions": [
            {
              "question": "string",
              "answerRequired": "string"
            }
          ],
          "subCategoryHeader": "string",
          "subcategories": [
            {
              "category": "string",
              "label": "string",
              "disabled": "boolean",
              "filterValue": "string",
              "evidenceGroupsToInclude": [
                "EvidenceGroup"
              ],
              "evidenceCategoriesToExclude": [
                "string"
              ],
              "feeWaivable": "boolean",
              "applicationReasonCode": "string",
              "benefitTypeCodeOverride": "string",
              "immigrationClassCode": "string",
              "actionRequestedOptions": [
                {
                  "category": "string",
                  "label": "string",
                  "evidenceCategoriesToExclude": [
                    "string"
                  ]
                }
              ]
            }
          ],
          "disabled": "boolean",
          "roleTypes": [
            "string"
          ],
          "excludeForParentFormTypes": [
            "string"
          ]
        },
        "eligibilitySubcategories": [
          "EligibilitySubcategory"
        ]
      }
    ],
    "pages": {
      "beforeYouStart": {
        "title": "string",
        "description": "string",
        "beforeYouStartCategories": [
          {
            "title": "string",
            "body": "string",
            "icon": "string",
            "accordionCategories": [
              {
                "title": "string",
                "id": "string",
                "body": "string"
              }
            ],
            "orgSpecificText": "string"
          }
        ],
        "afterYouFileCategories": [
          "ContentCategory"
        ]
      },
      "completeYourForm": {
        "title": "string",
        "categories": [
          "ContentCategory"
        ],
        "notices": [
          "ContentCategory"
        ]
      },
      "eligibility": {
        "header": "string"
      },
      "uploadForm": {
        "additionalInfoPartNumber": "string",
        "optionalLinkText": "string",
        "optionalLink": "string",
        "pageMarkdown": "string"
      }
    },
    "landmarks": {
      "fieldOrSectionName": "integer"
    },
    "errorMap": [
      {
        "errorType": "enum: PAGE_ERROR | PAGE_NUMBER_ERROR | PAGE_ORDER_ERROR | FORM_TYPE_ERROR | FORM_VERSION_ERROR | SIGNATURE_ERROR | BARCODE_ERROR | UNEXPECTED_FORM_TYPE | UNSUPPORTED_FORM_EDITION_ERROR | TOTAL_PAGE_NUMBER_ERROR | A_NUMBER_ERROR | DOB_ERROR",
        "errorMessage": "string"
      }
    ],
    "fees": {
      "feeName": "integer"
    },
    "concurrentFormTypes": [
      "string"
    ],
    "eligibilityWarningText": {
      "headerText": "string",
      "body": "string",
      "enabled": "boolean"
    }
  }
}
```

</details>

---

<details>
<summary>dev-PaygovTransaction</summary>

```json
{
  "_table": "dev-PaygovTransaction",
  "_keys": {
    "partition": "id"
  },
  "_indexes": {
    "by_agencyTrackingId": {
      "partition": "agencyTrackingId",
      "projection": "ALL"
    }
  },
  "_source": {
    "terraform": "tools/test-doubles/paygov/devops/terraform/app/dynamodb.tf",
    "javaBinding": "tools/test-doubles/paygov/src/main/java/gov/dhs/testdouble/paygov/PaygovTransaction.java"
  },
  "item": {
    "id": "string",
    "agencyTrackingId": "string",
    "tcsAppId": "string",
    "amount": "string",
    "date": "localDate",
    "status": "string"
  },
  "_migrationNotes": [
    "Development/test-double table only; exclude from production applicant-data migration unless PayGov mock history is explicitly required."
  ]
}
```

</details>

---

<details>
<summary>dev-Submission</summary>

```json
{
  "_table": "dev-Submission",
  "_keys": {
    "partition": "submissionId"
  },
  "_indexes": {
    "requests_by_myUscisId": {
      "partition": "myUscisId",
      "projection": "ALL"
    },
    "requests_by_orgMyUscisId": {
      "partition": "orgMyUscisId",
      "projection": "ALL"
    },
    "requests_by_manifestId": {
      "partition": "manifestId",
      "projection": "ALL"
    },
    "requests_by_formType": {
      "partition": "formType",
      "projection": "ALL"
    },
    "requests_by_lockboxStatus": {
      "partition": "lockboxStatus",
      "projection": "ALL"
    },
    "requests_by_status": {
      "partition": "status",
      "projection": "ALL"
    }
  },
  "item": {
    "submissionId": "string",
    "myUscisId": "string",
    "orgMyUscisId": "string",
    "formType": "enum: FormType",
    "formName": "string",
    "eligibilityCategory": "enum: EligibilityCategoryEnum",
    "eligibilitySubcategory": "enum: EligibilitySubcategoryEnum",
    "eligibilitySubcategories": [
      "enum: EligibilitySubcategoryEnum"
    ],
    "benefitTypeCode": "integer",
    "applicationReaonCode": "integer",
    "manifestId": "string",
    "status": "enum: DRAFT | DELETED | PUBLISHED | ACCEPTED | REJECTED | RECEIVED | ERROR",
    "lockboxStatus": "enum: RECEIVED | REJECTED | ACCEPTED | PROCESSING | PENDING | ERROR",
    "lockboxErrors": [
      {
        "id": "string",
        "errorCode": "string",
        "description": "string"
      }
    ],
    "feeTotal": "decimal",
    "timestamp": "zonedDateTime",
    "updatedAt": "zonedDateTime",
    "lockedAt": "zonedDateTime",
    "submittedAt": "zonedDateTime",
    "forms": [
      {
        "type": "string",
        "eligibilityCategory": "enum: EligibilityCategoryEnum",
        "eligibilitySubcategory": "enum: EligibilitySubcategoryEnum",
        "benefitTypeCode": "integer",
        "applicationReasonCode": "integer",
        "eligibilitySubcategoryList": [
          "enum: EligibilitySubcategoryEnum"
        ],
        "id": "string",
        "url": "string",
        "name": "string",
        "feeTotal": "decimal",
        "paymentTrackingId": "string",
        "md5Hash": "string",
        "lockboxStatus": "string",
        "validations": [
          "string"
        ],
        "rejectionReasons": [
          {
            "code": "string",
            "reason": "string"
          }
        ],
        "uploadedAt": "zonedDateTime",
        "s3Key": "string",
        "markedS3Key": "string",
        "attestedS3Key": "string",
        "attestationStatus": "enum: SUCCESS | FAILURE | IN_PROGRESS",
        "extractedData": {
          "eligibilityCategory": "string",
          "isSalvadorianABCEligible": "boolean",
          "reasonForFiling": "string",
          "eligibilitySubcategories": [
            "string"
          ]
        },
        "formSpecificInfo": "JSON string containing FormSpecificInfo subtype selected by formType",
        "clientId": "string",
        "clientName": {
          "firstName": "string",
          "middleName": "string",
          "lastName": "string",
          "organizationName": "string"
        }
      }
    ],
    "evidences": [
      {
        "type": "string",
        "evidenceGroupType": "string",
        "id": "string",
        "formId": "string",
        "url": "string",
        "name": "string",
        "md5Hash": "string",
        "uploadedAt": "zonedDateTime",
        "s3Key": "string",
        "markedS3Key": "string",
        "isEditable": "boolean"
      }
    ],
    "feeWaiver": {
      "type": "string",
      "evidenceGroupType": "string",
      "id": "string",
      "formId": "string",
      "url": "string",
      "name": "string",
      "md5Hash": "string",
      "uploadedAt": "zonedDateTime",
      "s3Key": "string",
      "markedS3Key": "string",
      "isEditable": "boolean"
    },
    "userRequestedFeeWaiver": "boolean",
    "userRequestedPartialFeeWaiver": "boolean",
    "userIsEligibleForFeeWaiver": "boolean",
    "attested": "boolean",
    "attestedAt": "zonedDateTime",
    "attestationSignature": "string",
    "hasReviewedSignature": "boolean",
    "immvi": "boolean",
    "isUSCitizen": "enum: ResidencyStatus",
    "c9updateRequest": {
      "options": {
        "onOrAfterApril012024": "boolean",
        "beforeApril012024": "boolean"
      },
      "isFeeWaiverEligible": "boolean"
    },
    "reasonForFiling": "string",
    "transmissionStatus": {
      "status": "enum: SUCCESS | FAILURE | IN_PROGRESS",
      "message": "string"
    },
    "applicant": {
      "myUscisId": "string",
      "userGroupId": "string",
      "email": "string",
      "accountType": "enum: APPLICANT | REPRESENTATIVE | REGISTRANT"
    },
    "formSpecificInfo": "JSON string containing one of the FormSpecificInfo subtypes",
    "sentNotify": "boolean",
    "selectedConcurrentForms": [
      "enum: FormType"
    ],
    "clients": "JSON string containing array of Client objects"
  },
  "_notPersisted": {
    "clientInformation": {
      "_reason": "Submission.clientInformation getter is annotated @DynamoDbIgnore.",
      "_sourceTable": "dev-ClientInformation",
      "_join": "Submission.submissionId = ClientInformation.submissionId",
      "_shape": "See dev-ClientInformation JSON structure."
    }
  },
  "_convertedJsonStringFields": {
    "formSpecificInfo": {
      "converter": "FormSpecificInfoConverter",
      "storedAs": "DynamoDB string containing JSON",
      "parsedShape": "One of the FormSpecificInfo subtypes listed below."
    },
    "forms[].formSpecificInfo": {
      "converter": "FormSpecificInfoConverter",
      "storedAs": "DynamoDB string containing JSON",
      "parsedShape": "One of the FormSpecificInfo subtypes listed below."
    },
    "clients": {
      "converter": "ClientConverter",
      "storedAs": "DynamoDB string containing JSON array",
      "parsedShape": [
        {
          "uuid": "string",
          "name": "string",
          "firstName": "string",
          "lastName": "string",
          "organizationName": "string",
          "formsToFile": [
            "string"
          ],
          "role": "string",
          "email": "string",
          "dateOfBirth": "string",
          "aNumber": "string"
        }
      ]
    }
  },
  "_formSpecificInfoSubtypes": {
    "common": {
      "formType": "enum: FormType"
    },
    "actionRequestedForms": [
      "I-90",
      "I-129CW",
      "I-129E",
      "I-129E3",
      "I-129H1B",
      "I-129H2B",
      "I-129H3",
      "I-129L",
      "I-129OP",
      "I-129Q",
      "I-129R",
      "I-129S",
      "I-129TN"
    ],
    "fieldsBySubtype": {
      "I-129H1B": [
        "actionRequested",
        "isCapEnabled",
        "isCapGap"
      ],
      "I-129H2A": [
        "requestedAction"
      ],
      "I-130": [
        "isUSCitizen"
      ],
      "I-140": [
        "dolNumber",
        "isScheduleA"
      ],
      "I-485": [
        "isMilitaryVeteran",
        "isFamilyBased",
        "isUnder14"
      ],
      "I-485A": [
        "isApplicantUnder17Unmarried",
        "isSpouseOfLegalizedAlien",
        "isUnmarriedChildUnder21"
      ],
      "I-539": [
        "isOnlyApplicantApplying"
      ],
      "I-751": [
        "isUnderOrders"
      ],
      "I-765": [
        "underABCAgreement",
        "before04012024",
        "onOrAfterApril012024",
        "feeWaiverRequested",
        "immvi",
        "reasonForFiling"
      ],
      "I-907": [
        "underlyingPetitionReceiptNumber"
      ],
      "N-400": [
        "isDisabledOrImpaired"
      ]
    }
  }
}
```

</details>

---

<details>
<summary>dev-SubmissionManifest</summary>

```json
{
  "_table": "dev-SubmissionManifest",
  "_keys": {
    "partition": "manifestId"
  },
  "_indexes": {},
  "item": {
    "manifestId": "string",
    "myUscisId": "string",
    "submissionId": "string",
    "statuses": {
      "manifestStatus": {
        "status": "enum: COMPLETE | ACCEPTED | REJECTED | RECEIVED | ERROR | PUBLISHED | PENDING | MIXED | COULD_NOT_PROCESS",
        "errors": [
          {
            "id": "string",
            "errorCode": "string",
            "description": "string"
          }
        ]
      },
      "submissionStatus": {
        "correlationId": "string",
        "uscisId": "string",
        "status": "enum: COMPLETE | ACCEPTED | REJECTED | RECEIVED | ERROR | PUBLISHED | PENDING | MIXED | COULD_NOT_PROCESS",
        "applicationStatuses": [
          {
            "uscisId": "string",
            "formType": "string",
            "status": "enum: COMPLETE | ACCEPTED | REJECTED | RECEIVED | ERROR | PUBLISHED | PENDING | MIXED | COULD_NOT_PROCESS",
            "uscisReceiptNumber": "string",
            "rejectionReasons": [
              {
                "code": "string",
                "reason": "string"
              }
            ]
          }
        ],
        "unableToProcessReason": [
          {
            "code": "string",
            "reason": "string"
          }
        ],
        "totalCost": "decimal"
      }
    },
    "manifest": {
      "manifestId": "string",
      "submissionDate": "string",
      "feeTotal": "decimal",
      "applications": [
        {
          "formType": "string",
          "submissionId": "string",
          "files": [
            {
              "order": "integer",
              "id": "string",
              "name": "string",
              "uri": "string",
              "md5Hash": "string"
            }
          ],
          "supportDocuments": [
            {
              "supportId": "string",
              "category": "string",
              "files": [
                "ApplicationFile"
              ]
            }
          ],
          "additionalInfo": [
            {
              "name": "string",
              "value": "string"
            }
          ]
        }
      ]
    }
  },
  "_jsonApiAliases": {
    "item.statuses.submissionStatus.applicationStatuses[].uscisId": "id"
  }
}
```

</details>

---

<details>
<summary>dev-SubmissionSummary</summary>

```json
{
  "_table": "dev-SubmissionSummary",
  "_keys": {
    "partition": "submissionId"
  },
  "_indexes": {
    "summary_by_manifestId": {
      "partition": "manifestId",
      "projection": "ALL"
    }
  },
  "item": {
    "submissionId": "string",
    "manifestId": "string"
  },
  "_relationship": "Created transactionally with dev-SubmissionManifest by SubmissionManifestRepository.saveSubmissionManifest."
}
```

</details>

---

<details>
<summary>dev-TextractJob</summary>

```json
{
  "_table": "dev-TextractJob",
  "_keys": {
    "partition": "jobId"
  },
  "_indexes": {},
  "item": {
    "jobId": "string",
    "documentId": "string",
    "eligibiltyCategory": "string",
    "submissionId": "string",
    "jobTypeEnum": "enum: I797_VALIDATION | I912_ALL_VALIDATIONS | I912_EDITION_VALIDATION | I912_SIGNATURE_VALIDATION | APPLICANT_SIGNATURE | REPRESENTATIVE_SIGNATURE | I765_NAME_EXTRACT | N400_NAME_EXTRACT | G28_NAME_EXTRACT",
    "jobStatus": "enum: IN_PROGRESS | SUCCEEDED | FAILED | PARTIAL_SUCCESS",
    "formType": "enum: FormType",
    "formVersion": "string"
  },
  "_migrationNotes": [
    "Java field name is eligibiltyCategory, preserving the misspelling in the persisted attribute name.",
    "Join to dev-Submission through submissionId; a submission can have multiple Textract jobs."
  ]
}
```

</details>

---

<details>
<summary>dev-ValidationStatus</summary>

```json
{
  "_table": "dev-ValidationStatus",
  "_keys": {
    "partition": "formId",
    "sort": "validator"
  },
  "_indexes": {},
  "item": {
    "formId": "string",
    "submissionId": "string",
    "status": "enum: PASS | FAIL | WARN | PENDING",
    "validator": "enum: CORE | SIGNATURE | ELIGIBILITY_CATEGORY | REASON_FOR_FILING | PERSON | NOA | I797C | I912 | E9089FD | I864 | I864EZ | E9142BFD | G28_COMP | G28_COMP_NAME | G28_NAME_EMAIL | G28_ATTORNEY_NAME | PFVS | ESIGN | N648",
    "failureReasons": [
      "string"
    ],
    "validationWarnings": {
      "warningCode": "warningMessage"
    },
    "messageId": "string",
    "isMultipart": "boolean"
  },
  "_migrationNotes": [
    "The primary key is formId + validator, not submissionId.",
    "Join to dev-Submission through the non-key submissionId attribute; a submission can have many validation rows."
  ]
}
```

</details>

---

<details>
<summary><b>Relationships between tables</b></summary>

`dev-Submission` is the migration spine. Most operational tables either join directly by `submissionId` or join through `manifestId` after the submission is sent to lockbox.

| From | To | Join key(s) | Cardinality | Notes |
|------|----|-------------|-------------|-------|
| `dev-Submission` | `dev-SubmissionManifest` | `LOWER(Submission.manifestId) = LOWER(SubmissionManifest.manifestId)` | `1:0..1` | `SubmissionRepository.getByManifestId` and lockbox reads normalize manifest IDs to lowercase. |
| `dev-Submission` | `dev-SubmissionSummary` | `Submission.submissionId = SubmissionSummary.submissionId` | `1:0..1` | Summary is a lockbox bridge row. |
| `dev-SubmissionSummary` | `dev-SubmissionManifest` | `SubmissionSummary.manifestId = SubmissionManifest.manifestId` | `N:1` | `summary_by_manifestId` supports reverse lookup. |
| `dev-Submission` | `dev-Audit` | `Submission.myUscisId = Audit.myUscisId` and `Submission.submissionId = Audit.submissionId` | `1:0..1` | Audit embeds many events in `events[]`; flatten to an audit-event fact table for SQL. |
| `dev-Submission` | `dev-ClientInformation` | `Submission.submissionId = ClientInformation.submissionId` | `1:0..1` | Used for representative/client submissions. `by_clientId` finds all submissions for a client. |
| `dev-Submission` | `dev-ValidationStatus` | `Submission.submissionId = ValidationStatus.submissionId` | `1:N` | Validation PK is `formId + validator`; `submissionId` is a join attribute. |
| `dev-Submission.forms[]` | `dev-ValidationStatus` | `Submission.forms[].id = ValidationStatus.formId` | `1:N` | File/form-level validation rows. |
| `dev-Submission` | `dev-TextractJob` | `Submission.submissionId = TextractJob.submissionId` | `1:N` | Textract jobs also carry `documentId`, typically a form/evidence document identifier. |
| `dev-Submission` | `dev-AddressExtract` | `Submission.submissionId = AddressExtract.submissionId` | `1:0..1` | Terraform/local table exists, but no Java entity was found. |
| `dev-Submission` | `dev-FormMetaData` | `Submission.formType = FormMetaData.formType` | `N:1` | Configuration/dimension lookup, not submission-owned data. |
| `dev-Submission.formType` / `dev-Submission.feeWaiver` | `dev-FeeWaiverMetaData` | Fee waiver form type, usually `I-912` | `N:1` | Lookup used by I-912 validators and fee-waiver flow. |
| `dev-Submission.evidences[].evidenceGroupType` | `dev-EvidenceMetaData` | Evidence metadata id / evidence page key | `N:1` | Evidence metadata is configuration for upload pages and evidence groups. |
| `dev-Submission` / payment flow | `dev-PaygovTransaction` | `agencyTrackingId`, `paymentTrackingId`, or mock payment IDs when present | Environment-specific | Dev/test-double table only; not part of core production applicant-data migration. |
