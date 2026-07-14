locals {
  dynamodb_tables = {
    departments = {
      suffix        = "departments"
      partition_key = { name = "id", type = "S" }
      indexes = [
        { name = "departments-by-code", partition_key = { name = "code", type = "S" } },
        { name = "departments-catalog", partition_key = { name = "entityType", type = "S" }, sort_key = { name = "createdAtId", type = "S" } }
      ]
    }
    students = {
      suffix        = "students"
      partition_key = { name = "id", type = "S" }
      indexes = [
        { name = "students-by-number", partition_key = { name = "studentNumber", type = "S" } },
        { name = "students-by-email", partition_key = { name = "email", type = "S" } },
        { name = "students-by-department", partition_key = { name = "departmentId", type = "S" }, sort_key = { name = "lastNameId", type = "S" } },
        { name = "students-by-status", partition_key = { name = "status", type = "S" }, sort_key = { name = "updatedAtId", type = "S" } },
        { name = "students-catalog", partition_key = { name = "entityType", type = "S" }, sort_key = { name = "createdAtId", type = "S" } }
      ]
    }
    student_profiles = {
      suffix        = "student-profiles"
      partition_key = { name = "studentId", type = "S" }
      indexes       = []
    }
    instructors = {
      suffix        = "instructors"
      partition_key = { name = "id", type = "S" }
      indexes = [
        { name = "instructors-by-number", partition_key = { name = "employeeNumber", type = "S" } },
        { name = "instructors-by-email", partition_key = { name = "email", type = "S" } },
        { name = "instructors-by-department", partition_key = { name = "departmentId", type = "S" }, sort_key = { name = "lastNameId", type = "S" } },
        { name = "instructors-catalog", partition_key = { name = "entityType", type = "S" }, sort_key = { name = "createdAtId", type = "S" } }
      ]
    }
    courses = {
      suffix        = "courses"
      partition_key = { name = "id", type = "S" }
      indexes = [
        { name = "courses-by-code", partition_key = { name = "courseCode", type = "S" } },
        { name = "courses-by-department", partition_key = { name = "departmentId", type = "S" }, sort_key = { name = "courseCodeId", type = "S" } },
        { name = "courses-by-instructor", partition_key = { name = "instructorId", type = "S" }, sort_key = { name = "courseCodeId", type = "S" } },
        { name = "courses-by-status", partition_key = { name = "status", type = "S" }, sort_key = { name = "updatedAtId", type = "S" } },
        { name = "courses-catalog", partition_key = { name = "entityType", type = "S" }, sort_key = { name = "createdAtId", type = "S" } }
      ]
    }
    enrollments = {
      suffix        = "enrollments"
      partition_key = { name = "id", type = "S" }
      indexes = [
        { name = "enrollments-by-student", partition_key = { name = "studentId", type = "S" }, sort_key = { name = "enrolledAtId", type = "S" } },
        { name = "enrollments-by-course", partition_key = { name = "courseId", type = "S" }, sort_key = { name = "enrolledAtId", type = "S" } },
        { name = "enrollments-by-status", partition_key = { name = "status", type = "S" }, sort_key = { name = "enrolledAtId", type = "S" } },
        { name = "enrollments-catalog", partition_key = { name = "entityType", type = "S" }, sort_key = { name = "enrolledAtId", type = "S" } }
        , { name = "enrollment-relationships-by-student", partition_key = { name = "relationshipStudentId", type = "S" }, sort_key = { name = "relationshipCourseId", type = "S" } }
        , { name = "enrollment-relationships-by-course", partition_key = { name = "relationshipCourseId", type = "S" }, sort_key = { name = "relationshipStudentId", type = "S" } }
      ]
    }
  }
}

module "student_portal_dynamodb" {
  for_each = local.dynamodb_tables
  source   = "../modules/dynamodb"

  table_name               = "${var.table_prefix}-${each.value.suffix}"
  partition_key            = each.value.partition_key
  global_secondary_indexes = each.value.indexes
  tags                     = merge(local.common_tags, { Domain = each.key })
}
