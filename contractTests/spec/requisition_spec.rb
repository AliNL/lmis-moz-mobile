# NOTE: API does not have delete endpoint and therefore can only
# post until reaching the current period. Use UAT env because UAT will
# clean up data regularly
describe "submit requisition to web server" do

  it "should submit successfully and return expected response" do

    requisition = {
      programCode: "ESS_MEDS",
      agentCode: "F10",
      products: [
        {
        productCode: "P74",
        beginningBalance: 1000,
        quantityReceived: 2000,
        quantityDispensed: 2500,
        totalLossesAndAdjustments: 0,
        stockInHand: 500,
        newPatientCount: 500,
        stockOutDays: 20,
        quantityRequested: 10000,
        reasonForRequestedQuantity: "justbecause"
        }
      ],
      patientQuantification: [
      {
        category: "adult",
        value: 100
      },
      {
        category: "child",
        value: 300
      }
      ],
      regimens: [
      {
        code: "001",
        name: "REGIMEN1",
        patientsOnTreatment: 200,
        patientsToInitiateTreatment: 200,
        patientsStoppedTreatment: 200,
        patientsOnTreatmentAdult:100,
        patientsToInitiateTreatmentAdult:100,
        patientsStoppedTreatmentAdult:100,
        patientsOnTreatmentChildren:100,
        patientsToInitiateTreatmentChildren:100,
        patientsStoppedTreatmentChildren:100,
        remarks:"remark"
      },
      {
        code: "002",
        name: "REGIMEN2",
        patientsOnTreatment: 200,
        patientsToInitiateTreatment: 200,
        patientsStoppedTreatment: 200,
        patientsOnTreatmentAdult:100,
        patientsToInitiateTreatmentAdult:100,
        patientsStoppedTreatmentAdult:100,
        patientsOnTreatmentChildren:100,
        patientsToInitiateTreatmentChildren:100,
        patientsStoppedTreatmentChildren:100,
        remarks:"remark"
      },
      {
        code: "003",
        name: "REGIMEN3",
        patientsOnTreatment: 200,
        patientsToInitiateTreatment: 200,
        patientsStoppedTreatment: 200,
        patientsOnTreatmentAdult:100,
        patientsToInitiateTreatmentAdult:100,
        patientsStoppedTreatmentAdult:100,
        patientsOnTreatmentChildren:100,
        patientsToInitiateTreatmentChildren:100,
        patientsStoppedTreatmentChildren:100,
        remarks:"remark"
      },
      {
        code: "005",
        name: "REGIMEN5",
        patientsOnTreatment: 200,
        patientsToInitiateTreatment: 200,
        patientsStoppedTreatment: 200,
        patientsOnTreatmentAdult:100,
        patientsToInitiateTreatmentAdult:100,
        patientsStoppedTreatmentAdult:100,
        patientsOnTreatmentChildren:100,
        patientsToInitiateTreatmentChildren:100,
        patientsStoppedTreatmentChildren:100,
        remarks:"remark"
      },
      {
        code:"006",
        name:"REGIMEN6",
        patientsOnTreatment: 200,
        patientsToInitiateTreatment: 200,
        patientsStoppedTreatment: 200,
        patientsOnTreatmentAdult:100,
        patientsToInitiateTreatmentAdult:100,
        patientsStoppedTreatmentAdult:100,
        patientsOnTreatmentChildren:100,
        patientsToInitiateTreatmentChildren:100,
        patientsStoppedTreatmentChildren:100,
        remarks:"remark"
      }
      ]
    }


    response = RestClient.post "http://#{WEB_UAT_URI}/rest-api/requisitions",
      requisition.to_json, 'Content-Type' => 'application/json',
      'Accept' => 'application/json',
      'Authorization' => http_basic_auth('superuser', 'password1')

    expect(response.code).to eq 201

    body = JSON.parse(response.body)
    requisition_id = body['requisitionId']

    expect(requisition_id).not_to be_nil

    response = RestClient.get "http://#{WEB_UAT_URI}/rest-api/requisitions/#{requisition_id}",
      'Content-Type' => 'application/json',
      'Accept' => 'application/json',
      'Authorization' => http_basic_auth('superuser', 'password1')

    expect(response.code).to eq 200

    body = JSON.parse(response.body)

    expect(body['requisition']['id']).to eq requisition_id
    expect(body['requisition']['programCode']).to eq "ESS_MEDS"
    expect(body['requisition']['agentCode']).to eq "F10"
    expect(body['requisition']['emergency']).to be false
    expect(body['requisition']['requisitionStatus']).to eq "AUTHORIZED"
  end
end