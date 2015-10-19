describe "Sync stock card data" do

  it "should sync all stock card data to web server" do
    login_response = RestClient.post "http://#{WEB_DEV_URI}/rest-api/login",
      { username: 'superuser', password: 'password1' }.to_json,
      :content_type => :json,
      :accept => :json

    facility_id = JSON.parse(login_response.body)['userInformation']['facilityId']

    stock_card_data =
      [
      {
        facilityId: 408,
        productCode: "08S42",
        quantity: 1000,
        reasonName: "CLINIC_RETURN",
        occurred: "20151010T030530Z"
      },
      {
        facilityId: 408,
        productCode: "08S42",
        quantity: 500,
        reasonName: "TRANSFER_OUT",
        occurred: "20151014T030530Z"
      },
      {
        facilityId: 408,
        productCode: "08S42",
        quantity: 20,
        reasonName: "STOLEN",
        occurred: "20151015T030530Z"
      }
      ]

    response = RestClient.post "http://#{WEB_DEV_URI}/rest-api/facilities/#{facility_id}/stockCards",
      stock_card_data.to_json, :content_type => :json, :accept => :json,
      :authorization => http_basic_auth('superuser', 'password1')

    body = JSON.parse(response.body)
    expect(response.code).to eq 200
  end
end